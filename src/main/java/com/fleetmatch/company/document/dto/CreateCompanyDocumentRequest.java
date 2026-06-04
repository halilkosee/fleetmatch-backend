package com.fleetmatch.company.document.dto;

import com.fleetmatch.company.document.entity.DocumentType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCompanyDocumentRequest {

    private DocumentType documentType;

    private String fileName;

    private String fileUrl;
}