package com.hopestar.hfms.config;

import com.hopestar.hfms.common.exception.FileStorageException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Activates {@link FileStorageProperties} and guarantees the configured
 * storage directories (documents, invoices, backups) exist at startup, so
 * later modules (Student Documents, Invoice PDFs, Backup & Restore) can
 * assume the directories are already present rather than each
 * re-implementing directory-creation logic.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(FileStorageProperties.class)
@RequiredArgsConstructor
public class FileStorageConfig {

    private final FileStorageProperties fileStorageProperties;

    @PostConstruct
    public void initStorageDirectories() {
        createDirectoryIfMissing(fileStorageProperties.getDocumentsPath());
        createDirectoryIfMissing(fileStorageProperties.getInvoicesPath());
        createDirectoryIfMissing(fileStorageProperties.getBackupPath());
    }

    private void createDirectoryIfMissing(String rawPath) {
        Path path = Path.of(rawPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(path);
            log.info("Storage directory ready: {}", path);
        } catch (IOException ex) {
            throw new FileStorageException("Could not create storage directory: " + path, ex);
        }
    }
}
