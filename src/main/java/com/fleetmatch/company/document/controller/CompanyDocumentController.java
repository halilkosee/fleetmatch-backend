package com.fleetmatch.company.document.controller;

import com.fleetmatch.security.user.CustomUserDetails;
import jakarta.validation.Valid;
import com.fleetmatch.company.document.entity.DocumentType;
import com.fleetmatch.company.document.service.DocumentDownload;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.fleetmatch.company.document.dto.CompanyDocumentResponse;
import com.fleetmatch.company.document.dto.CreateCompanyDocumentRequest;
import com.fleetmatch.company.document.service.CompanyDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/company/documents")
@RequiredArgsConstructor
public class CompanyDocumentController {

    private final CompanyDocumentService companyDocumentService;

    @PostMapping("/{companyId}")
    public CompanyDocumentResponse createDocument(
            @PathVariable UUID companyId,
            @Valid @RequestBody CreateCompanyDocumentRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return companyDocumentService.createDocument(
                companyId,
                request,
                currentUser
        );
    }

    @PostMapping(
            value = "/{companyId}/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public CompanyDocumentResponse uploadDocument(
            @PathVariable UUID companyId,
            @RequestParam DocumentType documentType,
            @RequestParam MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return companyDocumentService.uploadDocument(
                companyId,
                documentType,
                file,
                currentUser
        );
    }

    @GetMapping("/{documentId}/download")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable UUID documentId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        DocumentDownload download =
                companyDocumentService.downloadDocument(documentId, currentUser);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .contentLength(download.fileSizeBytes())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + download.fileName() + "\""
                )
                .body(download.resource());
    }
}
