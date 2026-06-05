package com.fleetmatch.company.document.dto;

import com.fleetmatch.company.document.entity.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCompanyDocumentRequest {

    @NotNull
    private DocumentType documentType;

    @NotBlank
    private String fileName;

    @NotBlank
    private String fileUrl;
}