package com.fleetmatch.company.documents.dto;

import com.fleetmatch.company.documents.entity.DocumentReviewStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewCompanyDocumentRequest {

    @NotNull
    private DocumentReviewStatus reviewStatus;

    @Size(max = 2000)
    private String reviewNotes;
}
