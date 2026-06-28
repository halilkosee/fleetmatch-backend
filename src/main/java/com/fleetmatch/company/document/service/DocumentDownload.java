package com.fleetmatch.company.document.service;

import org.springframework.core.io.Resource;

public record DocumentDownload(
        Resource resource,
        String fileName,
        String contentType,
        long fileSizeBytes
) {
}
