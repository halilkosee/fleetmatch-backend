package com.fleetmatch.company.documents.service;

import org.springframework.core.io.Resource;

public record DocumentDownload(
        Resource resource,
        String fileName,
        String contentType,
        long fileSizeBytes
) {
}
