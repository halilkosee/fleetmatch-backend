package com.fleetmatch.company.document.service;

public record StoredDocument(
        String storageKey,
        String fileName,
        String fileUrl,
        String originalFileName,
        String contentType,
        long fileSizeBytes
) {
}
