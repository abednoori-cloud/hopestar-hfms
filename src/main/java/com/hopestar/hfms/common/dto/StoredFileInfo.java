package com.hopestar.hfms.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Result of a successful {@code FileStorageService.store(...)} call.
 * Feature modules (Student Documents today; Invoice PDFs and Backup
 * dumps in later phases) persist these fields onto their own entities
 * rather than re-deriving storage paths themselves.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoredFileInfo {

    /** Absolute path on disk where the file was written. */
    private String storedPath;

    /** Original filename as uploaded by the browser. */
    private String originalFileName;

    /** Detected/declared MIME type. */
    private String contentType;

    /** Size in bytes. */
    private long sizeBytes;
}
