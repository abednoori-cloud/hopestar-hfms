package com.hopestar.hfms.launcher;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Supervises the packaged HFMS desktop deployment: one-time MySQL setup,
 * starting the real Spring Boot app as a child process with the right
 * environment, waiting for it to come up, opening the browser, and a
 * system tray icon for the user to see it's running / shut it down.
 * <p>
 * This class -- not {@link com.hopestar.hfms.HfmsApplication} -- is what
 * jpackage's native launcher actually runs (see build-installer.ps1's
 * {@code --main-class}). It never touches Spring; it just execs the same
 * hfms.jar in a second JVM, letting that jar's normal manifest-driven
 * Spring Boot startup happen unchanged in the child process.
 * <p>
 * Runs with no console attached (jpackage app-image without
 * {@code --win-console}), so nothing written to stdout/stderr is ever
 * seen by the user -- every user-facing outcome must go through a Swing
 * dialog, the tray icon, or the browser. Anything unexpected is logged to
 * {@code %LOCALAPPDATA%\HopeStarHFMS\logs\launcher.log} and surfaced as a
 * plain-language dialog; nothing is allowed to fail silently.
 */
public final class Launcher {

    private static final String APP_URL = "https://localhost:8443";
    private static final String HEALTH_URL = APP_URL + "/actuator/health";
    private static final Duration HEALTH_TIMEOUT = Duration.ofSeconds(90);
    private static final Duration MYSQL_SETUP_TIMEOUT = Duration.ofMinutes(10);

    private final Path localAppData = Paths.get(System.getenv("LOCALAPPDATA"), "HopeStarHFMS");
    private final Path credentialsPath = localAppData.resolve("config").resolve("db.properties");
    private final Path logDir = localAppData.resolve("logs");

    // This class is packaged (and, at runtime, actually loaded) from a
    // separate launcher.jar sitting next to the real app -- see
    // build-installer.ps1 for why: jpackage's --main-class resolution
    // needs a flat classpath, which a Spring Boot repackaged fat jar
    // (BOOT-INF/classes/...) doesn't offer. So "my own jar" and "the
    // Spring Boot app to run" are deliberately two different files in
    // the same directory, not the same file.
    private final Path launcherJarPath = resolveOwnJar();
    private final Path appDir = launcherJarPath.getParent();
    private final Path springBootJarPath = appDir.resolve("hfms.jar");

    private volatile Process springBootProcess;
    private TrayIcon trayIcon;

    public static void main(String[] args) {
        new Launcher().run();
    }

    private void run() {
        try {
            Files.createDirectories(logDir);
        } catch (IOException e) {
            // Can't even create the log dir -- fall through, fatalError()
            // below still shows a dialog even if it can't also log.
        }

        try {
            if (!Files.exists(credentialsPath)) {
                log("First run detected (no credentials file at " + credentialsPath + "). Running MySQL setup...");
                runFirstTimeSetup();
            } else {
                log("Credentials file found at " + credentialsPath + " -- skipping MySQL setup.");
            }

            Properties creds = loadCredentials();
            springBootProcess = startSpringBootApp(creds);
            setupTrayIcon(); // visible immediately, tooltip says "starting"

            if (!waitForHealthy(HEALTH_TIMEOUT)) {
                throw new LauncherException(
                    "HopeStar HFMS did not finish starting in time.",
                    "Try closing HopeStar HFMS from the system tray (if shown) and opening it again. "
                        + "If this keeps happening, restart your computer and try once more, then contact support.");
            }

            updateTrayTooltip("HopeStar HFMS -- Running");
            openBrowser();

        } catch (LauncherException e) {
            fatalError(e.getUserMessage(), e.getUserDetail(), e);
        } catch (Exception e) {
            fatalError(
                "HopeStar HFMS ran into a problem it didn't expect and couldn't start.",
                "Please contact support. A technical log has been saved that can help fix this.",
                e);
        }
    }

    // ---------------------------------------------------------------
    // First-time MySQL setup: runs install-mysql-headless.ps1 elevated
    // (it needs admin rights to install MySQL as a Windows service --
    // that's a hard requirement from the MySQL side, not something this
    // launcher can avoid), hidden (no console window), with a plain-
    // language "please wait" window since it can take a few minutes.
    // ---------------------------------------------------------------
    private void runFirstTimeSetup() throws LauncherException {
        JDialog waitDialog = showSetupWaitDialog();
        try {
            Path script = appDir.resolve("install-mysql-headless.ps1");
            if (!Files.exists(script)) {
                throw new LauncherException(
                    "Setup could not find a required file.",
                    "install-mysql-headless.ps1 is missing from the app folder. Reinstalling HopeStar HFMS "
                        + "usually fixes this; if not, contact support.");
            }

            Path msiPath = appDir.resolve("mysql-installer").resolve("mysql-9.7-winx64.msi");
            Path stdOutLog = logDir.resolve("mysql-setup-stdout.log");
            Path stdErrLog = logDir.resolve("mysql-setup-stderr.log");

            // Overridable only for testing this exact code path against
            // throwaway names (see chat) -- defaults are the real
            // production database/user install-mysql-headless.ps1 uses
            // when these system properties aren't set, which is always
            // true for the actual packaged installer (jpackage's .cfg
            // never sets them).
            String dbName = System.getProperty("hfms.launcher.dbName", "hfms_prod");
            String dbUser = System.getProperty("hfms.launcher.dbUser", "hfms_user");

            // Start-Process's -Verb RunAs (elevation via ShellExecute) and
            // -RedirectStandardOutput/-RedirectStandardError (I/O via
            // CreateProcess) are mutually exclusive parameter sets --
            // combining them fails immediately with a ParameterBindingException
            // ("AmbiguousParameterSet"), *before* elevation is even attempted
            // (confirmed by testing -- see chat). So instead of redirecting
            // the elevated process's I/O from out here, we generate a small
            // wrapper script that redirects its OWN output internally (via
            // 1>/2>, not Start-Process parameters) and hand Start-Process
            // -Verb RunAs nothing but a single -File path to run -- no
            // redirection parameters on that call at all.
            // install-mysql-headless.ps1 logs its progress via Write-Host,
            // which writes to the Information stream (6) -- plain 1>/2>
            // redirection (success/error streams only) would silently
            // drop all of it. *> redirects every stream (success, error,
            // warning, verbose, debug, information) into one file, which
            // both captures everything and keeps it in real chronological
            // order.
            Path wrapperScript = logDir.resolve("run-mysql-setup.ps1");
            String wrapperContent = "$ErrorActionPreference = 'Stop'\r\n"
                + "try {\r\n"
                + "    & " + psLiteral(script.toString())
                + " -CredentialsPath " + psLiteral(credentialsPath.toString())
                + " -MsiPath " + psLiteral(msiPath.toString())
                + " -DbName " + psLiteral(dbName)
                + " -DbUser " + psLiteral(dbUser)
                + " *> " + psLiteral(stdOutLog.toString()) + "\r\n"
                + "    exit $LASTEXITCODE\r\n"
                + "} catch {\r\n"
                + "    ($_ | Out-String) | Add-Content -Path " + psLiteral(stdErrLog.toString()) + "\r\n"
                + "    exit 1\r\n"
                + "}\r\n";
            Files.writeString(wrapperScript, wrapperContent, StandardCharsets.UTF_8);

            // Computed here (in this normal, non-elevated process) rather
            // than left for the elevated script to resolve $env:LOCALAPPDATA
            // itself -- if the UAC prompt is ever answered under a
            // *different* admin account than the logged-in user, that
            // account's LOCALAPPDATA would silently differ from this one.
            // Passing it explicitly (via the wrapper script above) removes
            // that ambiguity entirely.
            //
            // The Start-Process call itself is now wrapped in try/catch:
            // any failure to even launch it (bad parameters, the user
            // declining the UAC prompt, etc.) is translated into an
            // explicit non-zero exit code instead of leaving $p unassigned
            // and silently `exit $null`-ing as 0 -- which is exactly the
            // bug that produced a false "success" last time (see chat).
            String inner = "try {\r\n"
                + "    $p = Start-Process -FilePath 'powershell.exe' -ArgumentList @("
                + "'-NoProfile','-ExecutionPolicy','Bypass','-WindowStyle','Hidden','-File',"
                + psLiteral(wrapperScript.toString())
                + ") -Verb RunAs -Wait -PassThru\r\n"
                + "    exit $p.ExitCode\r\n"
                + "} catch {\r\n"
                + "    if ($_.Exception.NativeErrorCode -eq 1223) { exit 1223 } else { exit 1618 }\r\n"
                + "}";

            List<String> outerCmd = List.of(
                "powershell.exe", "-NoProfile", "-WindowStyle", "Hidden", "-Command", inner);

            // The OUTER wrapper's own stdout/stderr -- distinct from the
            // inner elevated script's (stdOutLog/stdErrLog above). A
            // failure in the elevation mechanism itself (e.g. the
            // ParameterBindingException that caused this whole rewrite)
            // only shows up here, so this must never be discarded.
            Path outerStdOutLog = logDir.resolve("mysql-setup-outer-stdout.log");
            Path outerStdErrLog = logDir.resolve("mysql-setup-outer-stderr.log");

            log("Launching elevated MySQL setup via wrapper: " + wrapperScript);
            log("Wrapper script content:\n" + wrapperContent);
            log("Elevation command: " + inner);
            ProcessBuilder pb = new ProcessBuilder(outerCmd);
            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(outerStdOutLog.toFile()));
            pb.redirectError(ProcessBuilder.Redirect.appendTo(outerStdErrLog.toFile()));
            Process proc = pb.start();

            boolean finished = proc.waitFor(MYSQL_SETUP_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                proc.destroyForcibly();
                throw new LauncherException(
                    "First-time setup is taking much longer than expected.",
                    "Please try running HopeStar HFMS again. If this keeps happening, contact support.");
            }

            int exitCode = proc.exitValue();
            log("Elevated MySQL setup finished with exit code " + exitCode);

            if (exitCode == 1223) {
                // Standard Windows "the operation was canceled by the user"
                // code -- what Start-Process/UAC report when the user
                // clicks "No" on the permission prompt rather than "Yes".
                throw new LauncherException(
                    "Setup needs your permission to continue.",
                    "A Windows permission prompt appeared and wasn't approved. Please run HopeStar HFMS again "
                        + "and click \"Yes\" when Windows asks for permission.");
            }
            if (exitCode != 0) {
                throw new LauncherException(
                    "Setting up HopeStar HFMS for the first time didn't complete successfully.",
                    "Please contact support. Technical details were saved to:\n" + stdErrLog);
            }

            if (!Files.exists(credentialsPath)) {
                throw new LauncherException(
                    "Setup finished but something still isn't right.",
                    "Please contact support. Technical details were saved in:\n" + logDir);
            }

        } catch (LauncherException e) {
            throw e;
        } catch (Exception e) {
            throw new LauncherException(
                "Something went wrong while setting up HopeStar HFMS for the first time.",
                "Please contact support. A technical log has been saved that can help fix this.", e);
        } finally {
            waitDialog.dispose();
        }
    }

    private JDialog showSetupWaitDialog() {
        JDialog dialog = new JDialog((Frame) null, "HopeStar HFMS", false);
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        JLabel label = new JLabel("<html><div style='width:280px'>Setting up HopeStar HFMS for the first "
            + "time. This may take a few minutes -- please don't close this window.<br><br>"
            + "You may be asked for Windows permission during setup; please click \"Yes\" to continue.</div></html>");
        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        panel.add(label, BorderLayout.CENTER);
        panel.add(bar, BorderLayout.SOUTH);
        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setAlwaysOnTop(true);
        dialog.setResizable(false);
        dialog.setVisible(true);
        return dialog;
    }

    // Quotes a path as a single-quoted PowerShell string literal
    // (doubling any embedded single quote, the standard PS escape).
    private static String psLiteral(String value) {
        return "'" + value.replace("'", "''") + "'";
    }

    // ---------------------------------------------------------------
    // Reading credentials + starting the real app as a child process.
    // ---------------------------------------------------------------
    private Properties loadCredentials() throws LauncherException {
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(credentialsPath)) {
            props.load(in);
        } catch (IOException e) {
            throw new LauncherException(
                "HopeStar HFMS couldn't read its database settings.",
                "Please contact support. Technical details were saved in:\n" + logDir, e);
        }
        for (String key : List.of("DB_HOST", "DB_PORT", "DB_NAME", "DB_USERNAME", "DB_PASSWORD")) {
            if (props.getProperty(key) == null || props.getProperty(key).isBlank()) {
                throw new LauncherException(
                    "HopeStar HFMS's database settings look incomplete.",
                    "Please contact support. Technical details were saved in:\n" + logDir);
            }
        }
        // MYSQL_BIN_DIR is optional, not required like the DB_* keys above --
        // it's only present in db.properties written by a version of
        // install-mysql-headless.ps1 new enough to record it. An existing
        // install from before that change simply won't have it, and
        // MysqlToolLocator's runtime scan (see BackupConfig) is what covers
        // that case instead.
        return props;
    }

    private Process startSpringBootApp(Properties creds) throws LauncherException {
        String javaHome = System.getProperty("java.home");
        Path javaw = Paths.get(javaHome, "bin", "javaw.exe");

        List<String> cmd = List.of(javaw.toString(), "-jar", springBootJarPath.toString());
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(appDir.toFile());
        pb.environment().put("HFMS_PROFILE", "prod");
        pb.environment().put("DB_HOST", creds.getProperty("DB_HOST"));
        pb.environment().put("DB_PORT", creds.getProperty("DB_PORT"));
        pb.environment().put("DB_NAME", creds.getProperty("DB_NAME"));
        pb.environment().put("DB_USERNAME", creds.getProperty("DB_USERNAME"));
        pb.environment().put("DB_PASSWORD", creds.getProperty("DB_PASSWORD"));

        String mysqlBinDir = creds.getProperty("MYSQL_BIN_DIR");
        if (mysqlBinDir != null && !mysqlBinDir.isBlank()) {
            pb.environment().put("HFMS_MYSQLDUMP_PATH", Paths.get(mysqlBinDir, "mysqldump.exe").toString());
            pb.environment().put("HFMS_MYSQL_PATH", Paths.get(mysqlBinDir, "mysql.exe").toString());
        }

        Path appLog = logDir.resolve("app.log");
        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(appLog.toFile()));
        pb.redirectError(ProcessBuilder.Redirect.appendTo(appLog.toFile()));

        try {
            appendLogHeader(appLog);
            log("Starting Spring Boot app: " + cmd);
            return pb.start();
        } catch (IOException e) {
            throw new LauncherException(
                "HopeStar HFMS could not be started.",
                "Please contact support. Technical details were saved in:\n" + logDir, e);
        }
    }

    private boolean waitForHealthy(Duration timeout) {
        HttpClient client = trustingHttpClient();
        HttpRequest request = HttpRequest.newBuilder(URI.create(HEALTH_URL))
            .timeout(Duration.ofSeconds(3))
            .GET()
            .build();

        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (springBootProcess != null && !springBootProcess.isAlive()) {
                log("Spring Boot process exited early (code " + springBootProcess.exitValue() + ") while waiting for health check.");
                return false;
            }
            try {
                HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200 && resp.body().contains("\"UP\"")) {
                    return true;
                }
            } catch (Exception ignored) {
                // Not up yet -- expected while the app is still starting.
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private static HttpClient trustingHttpClient() {
        // Health check only, against our own self-signed cert on
        // localhost -- not used for anything else.
        try {
            TrustManager[] trustAll = {new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] c, String a) { }
                public void checkServerTrusted(X509Certificate[] c, String a) { }
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            }};
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, trustAll, new java.security.SecureRandom());
            return HttpClient.newBuilder().sslContext(ctx).connectTimeout(Duration.ofSeconds(3)).build();
        } catch (Exception e) {
            return HttpClient.newHttpClient();
        }
    }

    private void openBrowser() {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(APP_URL));
            } else {
                Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", APP_URL});
            }
        } catch (Exception e) {
            log("Could not open browser automatically: " + e);
        }
    }

    // ---------------------------------------------------------------
    // System tray.
    // ---------------------------------------------------------------
    private void setupTrayIcon() {
        if (!SystemTray.isSupported()) {
            log("SystemTray not supported on this system -- skipping tray icon.");
            return;
        }
        try {
            Image icon = loadTrayImage();
            PopupMenu menu = new PopupMenu();

            MenuItem open = new MenuItem("Open HopeStar HFMS");
            open.addActionListener(e -> openBrowser());
            menu.add(open);

            menu.addSeparator();

            MenuItem exit = new MenuItem("Exit");
            exit.addActionListener(e -> exitApplication());
            menu.add(exit);

            trayIcon = new TrayIcon(icon, "HopeStar HFMS -- Starting...", menu);
            trayIcon.setImageAutoSize(true);
            trayIcon.addActionListener(e -> openBrowser()); // double-click / default action
            SystemTray.getSystemTray().add(trayIcon);
        } catch (Exception e) {
            log("Could not set up system tray icon: " + e);
        }
    }

    private void updateTrayTooltip(String text) {
        if (trayIcon != null) {
            trayIcon.setToolTip(text);
        }
    }

    private Image loadTrayImage() throws IOException {
        try (InputStream in = Launcher.class.getResourceAsStream("/launcher/tray-icon.png")) {
            if (in == null) {
                // Fall back to a plain colored square rather than fail the
                // whole tray setup over a missing icon resource.
                Image fallback = new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                Graphics g = fallback.getGraphics();
                g.setColor(Color.BLUE);
                g.fillRect(0, 0, 16, 16);
                g.dispose();
                return fallback;
            }
            return Toolkit.getDefaultToolkit().createImage(in.readAllBytes());
        }
    }

    private void exitApplication() {
        log("Exit requested from tray menu.");
        stopSpringBootApp();
        if (trayIcon != null) {
            SystemTray.getSystemTray().remove(trayIcon);
        }
        System.exit(0);
    }

    private void stopSpringBootApp() {
        Process p = springBootProcess;
        if (p == null || !p.isAlive()) {
            return;
        }
        log("Stopping Spring Boot process (pid " + p.pid() + ")...");
        p.destroy();
        try {
            if (!p.waitFor(15, TimeUnit.SECONDS)) {
                log("Process did not exit within 15s of destroy() -- forcing.");
                p.destroyForcibly();
                p.waitFor(10, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            p.destroyForcibly();
        }
        log("Spring Boot process stopped (alive=" + p.isAlive() + ").");
    }

    // ---------------------------------------------------------------
    // Errors, logging, misc.
    // ---------------------------------------------------------------
    private void fatalError(String userMessage, String userDetail, Throwable cause) {
        log("FATAL: " + userMessage + " | " + userDetail);
        if (cause != null) {
            logStackTrace(cause);
        }
        stopSpringBootApp();
        String text = userMessage + (userDetail == null || userDetail.isBlank() ? "" : "\n\n" + userDetail);
        JOptionPane.showMessageDialog(null, text, "HopeStar HFMS", JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }

    private Path resolveOwnJar() {
        try {
            URI uri = Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            return Paths.get(uri);
        } catch (Exception e) {
            // Should be unreachable when actually run via `java -jar hfms.jar`.
            throw new IllegalStateException("Could not determine this launcher's own jar location.", e);
        }
    }

    private void log(String message) {
        String line = "[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "] " + message;
        try {
            Files.writeString(logDir.resolve("launcher.log"), line + System.lineSeparator(),
                StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
            // Logging is best-effort; never let a logging failure mask the real error.
        }
    }

    private void logStackTrace(Throwable t) {
        java.io.StringWriter sw = new java.io.StringWriter();
        t.printStackTrace(new java.io.PrintWriter(sw));
        log(sw.toString());
    }

    private void appendLogHeader(Path appLog) throws IOException {
        String header = "===== " + LocalDateTime.now() + " -- launching Spring Boot app =====" + System.lineSeparator();
        Files.writeString(appLog, header, StandardCharsets.UTF_8,
            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /** Carries a plain-language message (shown to the user) separately from technical detail (logged). */
    private static final class LauncherException extends Exception {
        private final String userMessage;
        private final String userDetail;

        LauncherException(String userMessage, String userDetail) {
            super(userMessage);
            this.userMessage = userMessage;
            this.userDetail = userDetail;
        }

        LauncherException(String userMessage, String userDetail, Throwable cause) {
            super(userMessage, cause);
            this.userMessage = userMessage;
            this.userDetail = userDetail;
        }

        String getUserMessage() { return userMessage; }
        String getUserDetail() { return userDetail; }
    }
}
