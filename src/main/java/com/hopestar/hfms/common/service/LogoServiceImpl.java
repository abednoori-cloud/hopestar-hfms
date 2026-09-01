package com.hopestar.hfms.common.service;

import com.hopestar.hfms.common.exception.PdfGenerationException;
import com.hopestar.hfms.module.auth.repository.BranchRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Optional;

/**
 * Implements {@link LogoService}. Serves the Organization Settings page's
 * uploaded logo (the headquarters {@code Branch.logoPath}, managed by
 * {@link FileStorageService}) when one has been uploaded, falling back to
 * the bundled {@code static/images/hopestar-logo.jpeg} classpath default
 * otherwise -- so PDF generation never breaks even before an admin has
 * uploaded a custom logo.
 * <p>
 * Only the classpath fallback is cached (once, at startup, since it never
 * changes at runtime); the uploaded logo is re-read from disk on every
 * call. That's deliberate: unlike the fallback, the uploaded logo can
 * change at any time via the Settings page, and a PDF generated right
 * after a new upload must show the new logo, not a startup-time snapshot.
 * A single small image read per PDF generation is not a meaningful cost
 * for this application's traffic.
 * <p>
 * Injects {@link BranchRepository} directly (a {@code common}-layer
 * service depending on a {@code module.auth} repository) -- the same
 * precedent {@link SequenceGeneratorServiceImpl} already established for
 * this exact codebase.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogoServiceImpl implements LogoService {

    private static final String LOGO_CLASSPATH_LOCATION = "static/images/hopestar-logo.jpeg";
    private static final String DEFAULT_MIME_TYPE = "image/jpeg";

    private final BranchRepository branchRepository;

    private String defaultLogoDataUri;

    @PostConstruct
    void loadDefaultLogo() {
        ClassPathResource resource = new ClassPathResource(LOGO_CLASSPATH_LOCATION);
        try {
            byte[] bytes = StreamUtils.copyToByteArray(resource.getInputStream());
            String base64 = Base64.getEncoder().encodeToString(bytes);
            this.defaultLogoDataUri = "data:" + DEFAULT_MIME_TYPE + ";base64," + base64;
        } catch (IOException ex) {
            throw new PdfGenerationException("Could not load the default logo from " + LOGO_CLASSPATH_LOCATION, ex);
        }
    }

    @Override
    public String getLogoDataUri() {
        return branchRepository.findFirstByHeadquartersTrueAndActiveTrue()
                .map(branch -> branch.getLogoPath())
                .flatMap(this::readUploadedLogo)
                .orElse(defaultLogoDataUri);
    }

    private Optional<String> readUploadedLogo(String logoPath) {
        if (logoPath == null) {
            return Optional.empty();
        }
        Path path = Path.of(logoPath);
        if (!Files.exists(path)) {
            log.warn("Branch.logoPath '{}' no longer exists on disk; falling back to the default logo.", logoPath);
            return Optional.empty();
        }
        try {
            byte[] bytes = Files.readAllBytes(path);
            String contentType = Files.probeContentType(path);
            if (contentType == null) {
                contentType = DEFAULT_MIME_TYPE;
            }
            String base64 = Base64.getEncoder().encodeToString(bytes);
            return Optional.of("data:" + contentType + ";base64," + base64);
        } catch (IOException ex) {
            log.warn("Could not read uploaded logo '{}'; falling back to the default logo.", logoPath, ex);
            return Optional.empty();
        }
    }
}
