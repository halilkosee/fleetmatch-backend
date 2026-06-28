package com.fleetmatch.company.document.service;

import com.fleetmatch.common.exception.BusinessRuleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class LocalDocumentStorageService implements DocumentStorageService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png"
    );

    private final Path baseDirectory;
    private final long maxFileSizeBytes;

    public LocalDocumentStorageService(
            @Value("${fleetmatch.documents.storage.local-path:./data/company-documents}") String localPath,
            @Value("${fleetmatch.documents.max-file-size-bytes:10485760}") long maxFileSizeBytes
    ) {
        this.baseDirectory = Path.of(localPath).toAbsolutePath().normalize();
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    @Override
    public StoredDocument store(UUID companyId, UUID documentId, MultipartFile file) {
        validate(file);

        String originalFileName = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "document" : file.getOriginalFilename()
        );
        String extension = extension(originalFileName);
        String storedFileName = documentId + extension;
        String storageKey = companyId + "/" + storedFileName;
        Path target = baseDirectory.resolve(storageKey).normalize();

        if (!target.startsWith(baseDirectory)) {
            throw new BusinessRuleException("Invalid document storage path");
        }

        try {
            Files.createDirectories(target.getParent());
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessRuleException("Document could not be stored");
        }

        return new StoredDocument(
                storageKey,
                storedFileName,
                "/api/company/documents/" + documentId + "/download",
                originalFileName,
                file.getContentType(),
                file.getSize()
        );
    }

    @Override
    public DocumentDownload load(String storageKey, String fileName, String contentType) {
        Path target = baseDirectory.resolve(storageKey).normalize();
        if (!target.startsWith(baseDirectory) || !Files.exists(target)) {
            throw new BusinessRuleException("Document file is not available");
        }

        return new DocumentDownload(
                new FileSystemResource(target),
                fileName,
                contentType == null || contentType.isBlank()
                        ? "application/octet-stream"
                        : contentType,
                target.toFile().length()
        );
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("Document file is required");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new BusinessRuleException("Document file is too large");
        }
        if (file.getContentType() == null ||
                !ALLOWED_TYPES.contains(file.getContentType().toLowerCase(Locale.ROOT))) {
            throw new BusinessRuleException("Only PDF, JPEG, and PNG documents are allowed");
        }
    }

    private String extension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        String extension = fileName.substring(dot).toLowerCase(Locale.ROOT);
        if (extension.length() > 10) {
            return "";
        }
        return extension;
    }
}
