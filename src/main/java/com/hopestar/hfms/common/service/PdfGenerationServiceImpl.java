package com.hopestar.hfms.common.service;

import com.hopestar.hfms.common.exception.PdfGenerationException;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Implements {@link PdfGenerationService} using openhtmltopdf (backed by
 * Apache PDFBox). No native dependencies, no headless browser -- pure JVM,
 * so it drops straight into the application jar.
 */
@Service
public class PdfGenerationServiceImpl implements PdfGenerationService {

    @Override
    public byte[] renderToPdf(String xhtml) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(xhtml, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (IOException ex) {
            throw new PdfGenerationException("Failed to render PDF", ex);
        }
    }
}
