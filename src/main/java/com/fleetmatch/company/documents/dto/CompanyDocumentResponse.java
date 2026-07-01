package com.fleetmatch.company.documents.dto;

import com.fleetmatch.company.documents.entity.DocumentType;
import com.fleetmatch.company.documents.entity.DocumentReviewStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class CompanyDocumentResponse {

    private UUID id;

    private DocumentType documentType;

    private String fileName;

    private String fileUrl;

    private String originalFileName;

    private String contentType;

    private Long fileSizeBytes;

    private DocumentReviewStatus reviewStatus;

    private String reviewNotes;

    private LocalDateTime reviewedAt;

    private LocalDateTime uploadedAt;
}
