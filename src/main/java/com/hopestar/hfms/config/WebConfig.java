package com.hopestar.hfms.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/**
 * General Spring MVC configuration.
 * <p>
 * Uploaded student documents are deliberately <b>not</b> exposed here as a
 * public static resource handler — per the Security Architecture (§5.3 of
 * the approved plan) they must only be served through an authenticated
 * download controller in the Student module. This class registers a
 * resource handler solely for the application's own static assets
 * (CSS/JS/images bundled in {@code src/main/resources/static}), which
 * Spring Boot would already serve by convention; it is declared explicitly
 * here as the designated place for any future custom resource-handling
 * rules (e.g. cache headers for generated invoice PDFs) so they don't end
 * up scattered across controllers.
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final FileStorageProperties fileStorageProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600);

        // Generated invoice PDFs are served through this handler once a
        // user is authenticated and authorized to view the given invoice;
        // the authorization check itself lives in the Invoice module's
        // controller layer, not here.
        String invoicesLocation = Path.of(fileStorageProperties.getInvoicesPath())
                .toAbsolutePath()
                .normalize()
                .toUri()
                .toString();
        registry.addResourceHandler("/files/invoices/**")
                .addResourceLocations(invoicesLocation)
                .setCachePeriod(0);
    }
}
