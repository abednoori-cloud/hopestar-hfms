package com.hopestar.hfms.common.service;

/**
 * Supplies the HopeStar logo for embedding in generated PDFs (Receipt,
 * Voucher). See {@link LogoServiceImpl} for why this is a base64 data URI
 * rather than a plain {@code <img src="...">} URL.
 */
public interface LogoService {

    /**
     * The logo as a {@code data:image/...;base64,...} URI, ready to use
     * directly as an {@code <img>} tag's {@code src}.
     */
    String getLogoDataUri();
}
