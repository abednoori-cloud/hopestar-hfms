package com.hopestar.hfms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * A second, independent Thymeleaf {@link TemplateEngine} used only for
 * rendering PDF source templates (receipts, vouchers, ...). Deliberately
 * separate from Spring Boot's autoconfigured {@code templateEngine} bean
 * (which stays in permissive {@code HTML} mode for every normal page in
 * the app): PDF templates are parsed in strict {@code XML} mode (Thymeleaf
 * 3.1 dropped the old dedicated {@code XHTML} mode -- {@code XML} is the
 * closest still-available equivalent), which fails fast on malformed
 * markup at render time instead of letting openhtmltopdf choke on it
 * later with a much less helpful error.
 * <p>
 * Templates for this engine live under the same {@code templates/}
 * classpath root as the rest of the app (e.g.
 * {@code templates/payments/receipt-pdf.html}) -- only the parsing mode
 * differs, not the location.
 * <p>
 * Built as a {@link SpringTemplateEngine} (not a bare {@code
 * TemplateEngine}) specifically so its standard dialect evaluates
 * expressions with SpringEL, matching every other template in the app --
 * a bare {@code TemplateEngine} defaults to OGNL, which is not on this
 * project's classpath (nothing else needs it, since {@code
 * spring-boot-starter-thymeleaf} wires every other template through
 * {@code SpringTemplateEngine}) and fails at render time with a
 * {@code NoClassDefFoundError} for {@code ognl.PropertyAccessor}.
 */
@Configuration
public class PdfTemplateConfig {

    @Bean
    public TemplateEngine pdfTemplateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.XML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(true);

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }
}
