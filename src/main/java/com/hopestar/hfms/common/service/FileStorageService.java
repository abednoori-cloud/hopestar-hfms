package com.hopestar.hfms.common.service;

import com.hopestar.hfms.common.dto.StoredFileInfo;
import org.springframework.web.multipart.MultipartFile;

/**
 * Validates and persists uploaded files to the storage roots configured by
 * {@link com.hopestar.hfms.config.FileStorageProperties} (established in
 * Phase 1). Every module that accepts an upload — Student Documents today,
 * Backup dumps in later phases — goes through this
 * service rather than touching the filesystem directly, per the approved
 * Security Architecture (§5.3): MIME-type allowlist, file-size cap, and
 * UUID-randomized filenames stored outside the web root.
 */
public interface FileStorageService {

    /**
     * Validates and stores an uploaded file under the given logical
     * sub-directory of the documents root (e.g. {@code "students/42"}).
     *
     * @throws com.hopestar.hfms.common.exception.FileStorageException if the
     *         file is empty, exceeds the configured size limit, has a
     *         disallowed content type, or cannot be written to disk
     */
    StoredFileInfo storeDocument(MultipartFile file, String subDirectory);

    /**
     * Deletes a previously stored file. Used only for cleanup after a
     * failed upload attempt — per the business rules, records that
     * reference a stored file are otherwise soft-deleted, never the file
     * itself.
     */
    void delete(String storedPath);
}
