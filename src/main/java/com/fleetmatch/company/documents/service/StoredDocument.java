package com.fleetmatch.company.documents.service;

public record StoredDocument(
        String storageKey,
        String fileName,
        String fileUrl,
        String originalFileName,
        String contentType,
        long fileSizeBytes
) {
}
