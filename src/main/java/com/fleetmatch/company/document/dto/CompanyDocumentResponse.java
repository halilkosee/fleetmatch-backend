package com.fleetmatch.company.document.dto;

import com.fleetmatch.company.document.entity.DocumentType;
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

    private LocalDateTime uploadedAt;
}