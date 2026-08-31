package com.hopestar.hfms.common.service;

import com.hopestar.hfms.common.exception.PdfGenerationException;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.util.Base64;

/**
 * Implements {@link LogoService} by reading {@code
 * static/images/hopestar-logo.jpeg} off the classpath and embedding it as
 * a base64 {@code data:} URI, computed once at startup and cached for the
 * life of the application.
 * <p>
 * This is deliberately <b>not</b> a normal {@code <img src="/images/...">}
 * URL, even though that path is what the browser-rendered navbar uses (see
 * {@code fragments/layout.html}): openhtmltopdf renders the Receipt/
 * Voucher HTML string in isolation, with no live HTTP request/response
 * cycle behind it -- there is no running "browser" to resolve a
 * server-relative URL against, and no {@code baseUri} is passed to {@code
 * PdfRendererBuilder.withHtmlContent} (see {@code PdfGenerationServiceImpl})
 * for it to resolve a relative or {@code classpath:} path against either.
 * A self-contained base64 data URI sidesteps all of that: the image bytes
 * travel inside the HTML string itself, so openhtmltopdf never needs to
 * fetch anything from the filesystem, classpath, or network to render it --
 * which also means it works identically whether the app is running from
 * exploded classes (IDE/dev) or a packaged jar (prod), where a
 * classpath resource is not a real file on disk at all.
 */
@Service
public class LogoServiceImpl implements LogoService {

    private static final String LOGO_CLASSPATH_LOCATION = "static/images/hopestar-logo.jpeg";
    private static final String MIME_TYPE = "image/jpeg";

    private String logoDataUri;

    @PostConstruct
    void loadLogo() {
        ClassPathResource resource = new ClassPathResource(LOGO_CLASSPATH_LOCATION);
        try {
            byte[] bytes = StreamUtils.copyToByteArray(resource.getInputStream());
            String base64 = Base64.getEncoder().encodeToString(bytes);
            this.logoDataUri = "data:" + MIME_TYPE + ";base64," + base64;
        } catch (IOException ex) {
            throw new PdfGenerationException("Could not load the HopeStar logo from " + LOGO_CLASSPATH_LOCATION, ex);
        }
    }

    @Override
    public String getLogoDataUri() {
        return logoDataUri;
    }
}
