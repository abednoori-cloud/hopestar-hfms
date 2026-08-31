package com.hopestar.hfms.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Binds the {@code hfms.file-storage.*} keys from application.yml.
 * Governs where uploaded student documents (passport, diploma, transcript,
 * English certificate, contract per SRS Module 3) and database backup
 * dumps are written, and the constraints enforced on uploads per the
 * approved architecture's Security Architecture §5.3.
 * <p>
 * {@link #receiptsPath} is reserved storage for a possible future
 * "save a permanent copy" feature; today's Receipt/Voucher PDFs (Student
 * Payments, Expenses) are generated on demand per download and never
 * written here -- see {@code PdfGenerationService}'s design notes.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "hfms.file-storage")
public class FileStorageProperties {

    /**
     * Root directory for uploaded student documents. Kept outside the web
     * root and served only through an authenticated download endpoint,
     * never as a static resource.
     */
    private String documentsPath = "./data/documents";

    /** Directory reserved for permanently-saved receipt/voucher PDFs, if ever needed. */
    private String receiptsPath = "./data/receipts";

    /** Directory where database backup dumps are written by default. */
    private String backupPath = "./data/backups";

    /** Maximum allowed upload size, in megabytes. */
    private int maxFileSizeMb = 10;

    /** Allowed MIME types for student document uploads. */
    private List<String> allowedDocumentTypes = List.of(
            "application/pdf",
            "image/jpeg",
            "image/png"
    );
}
