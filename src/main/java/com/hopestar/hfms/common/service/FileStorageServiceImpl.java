package com.hopestar.hfms.common.service;

import com.hopestar.hfms.common.dto.StoredFileInfo;
import com.hopestar.hfms.common.exception.FileStorageException;
import com.hopestar.hfms.config.FileStorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Default {@link FileStorageService} implementation. Stores files under
 * {@code hfms.file-storage.documents-path} (see {@link FileStorageProperties},
 * Phase 1), with:
 * <ul>
 *   <li>a MIME-type allowlist check,</li>
 *   <li>a configurable maximum file size,</li>
 *   <li>a UUID-randomized filename (the original filename is preserved
 *       only as metadata on the owning entity, never as the on-disk
 *       filename) to prevent path traversal and filename collisions.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private final FileStorageProperties fileStorageProperties;

    @Override
    public StoredFileInfo storeDocument(MultipartFile file, String subDirectory) {
        validate(file);

        Path targetDirectory = Path.of(fileStorageProperties.getDocumentsPath())
                .resolve(sanitizeSubDirectory(subDirectory))
                .toAbsolutePath()
                .normalize();

        try {
            Files.createDirectories(targetDirectory);
        } catch (IOException ex) {
            throw new FileStorageException("Could not create document storage directory: " + targetDirectory, ex);
        }

        String extension = extractExtension(file.getOriginalFilename());
        String storedFileName = UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);
        Path targetFile = targetDirectory.resolve(storedFileName).normalize();

        // Defense in depth: confirm the resolved path is still inside the
        // configured documents root before writing anything to disk.
        Path documentsRoot = Path.of(fileStorageProperties.getDocumentsPath()).toAbsolutePath().normalize();
        if (!targetFile.startsWith(documentsRoot)) {
            throw new FileStorageException("Resolved file path escapes the configured storage root.");
        }

        try (var inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new FileStorageException("Failed to store uploaded file: " + file.getOriginalFilename(), ex);
        }

        log.info("Stored document '{}' as '{}'", file.getOriginalFilename(), targetFile);

        return StoredFileInfo.builder()
                .storedPath(targetFile.toString())
                .originalFileName(StringUtils.cleanPath(file.getOriginalFilename() == null ? storedFileName : file.getOriginalFilename()))
                .contentType(file.getContentType())
                .sizeBytes(file.getSize())
                .build();
    }

    @Override
    public void delete(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return;
        }
        Path documentsRoot = Path.of(fileStorageProperties.getDocumentsPath()).toAbsolutePath().normalize();
        Path target = Path.of(storedPath).toAbsolutePath().normalize();
        if (!target.startsWith(documentsRoot)) {
            throw new FileStorageException("Refusing to delete a path outside the storage root: " + storedPath);
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException ex) {
            log.error("Failed to delete stored file: {}", storedPath, ex);
            throw new FileStorageException("Failed to delete stored file: " + storedPath, ex);
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("No file was provided for upload.");
        }
        long maxBytes = (long) fileStorageProperties.getMaxFileSizeMb() * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new FileStorageException(
                    "File exceeds the maximum allowed size of %d MB.".formatted(fileStorageProperties.getMaxFileSizeMb()));
        }
        String contentType = file.getContentType();
        if (contentType == null || !fileStorageProperties.getAllowedDocumentTypes().contains(contentType)) {
            throw new FileStorageException(
                    "File type '%s' is not allowed. Allowed types: %s"
                            .formatted(contentType, fileStorageProperties.getAllowedDocumentTypes()));
        }
    }

    private String sanitizeSubDirectory(String subDirectory) {
        if (subDirectory == null || subDirectory.isBlank()) {
            return "misc";
        }
        // Strip anything that could be used for path traversal; only
        // alphanumerics, dashes, underscores and forward slashes survive.
        return subDirectory.replaceAll("[^a-zA-Z0-9/_-]", "");
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }
        String cleaned = StringUtils.cleanPath(originalFilename);
        int dotIndex = cleaned.lastIndexOf('.');
        return dotIndex >= 0 && dotIndex < cleaned.length() - 1
                ? cleaned.substring(dotIndex + 1).toLowerCase()
                : "";
    }
}
