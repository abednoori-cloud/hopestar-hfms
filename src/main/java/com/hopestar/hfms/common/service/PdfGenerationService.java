package com.hopestar.hfms.common.service;

/**
 * Generic HTML-to-PDF rendering. Takes a well-formed XHTML string (e.g.
 * produced by rendering a Thymeleaf template in XHTML mode) and returns
 * PDF bytes -- has no knowledge of receipts, vouchers, or any other
 * specific document, so any future feature that needs a PDF can reuse it
 * rather than each one wiring up its own renderer.
 */
public interface PdfGenerationService {

    /**
     * Renders the given well-formed XHTML string to PDF.
     *
     * @throws com.hopestar.hfms.common.exception.PdfGenerationException if
     *         the markup cannot be parsed or the PDF cannot be written
     */
    byte[] renderToPdf(String xhtml);
}
