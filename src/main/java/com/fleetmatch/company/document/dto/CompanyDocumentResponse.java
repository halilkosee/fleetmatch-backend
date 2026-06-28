package com.fleetmatch.company.document.dto;

import com.fleetmatch.company.document.entity.DocumentType;
import com.fleetmatch.company.document.entity.DocumentReviewStatus;
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

    private DocumentReviewStatus reviewStatus;

    private String reviewNotes;

    private LocalDateTime reviewedAt;

    private LocalDateTime uploadedAt;
}
