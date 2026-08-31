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
 * rules so they don't end up scattered across controllers.
 * <p>
 * The {@code /files/receipts/**} handler below is reserved, currently-
 * unused scaffolding for a possible future "save a permanent copy of a
 * generated receipt/voucher" feature -- today's PDFs are generated on
 * demand and streamed directly from {@code StudentPaymentController}/
 * {@code ExpenseController}, never written to {@link
 * FileStorageProperties#getReceiptsPath()}. Note this handler is a plain
 * static resource mapping with no per-record ownership check beyond
 * {@code SecurityConfig}'s blanket "any authenticated user" rule -- if
 * this ever is wired up to serve real files, it needs the same
 * authenticated-download-controller treatment as student documents get,
 * not this resource handler as-is.
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

        String receiptsLocation = Path.of(fileStorageProperties.getReceiptsPath())
                .toAbsolutePath()
                .normalize()
                .toUri()
                .toString();
        registry.addResourceHandler("/files/receipts/**")
                .addResourceLocations(receiptsLocation)
                .setCachePeriod(0);
    }
}
