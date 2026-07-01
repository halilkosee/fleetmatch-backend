package com.fleetmatch.company.documents.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface DocumentStorageService {

    StoredDocument store(UUID companyId, UUID documentId, MultipartFile file);

    DocumentDownload load(String storageKey, String fileName, String contentType);
}
