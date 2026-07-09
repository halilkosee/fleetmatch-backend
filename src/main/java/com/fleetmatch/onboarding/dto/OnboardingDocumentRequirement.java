package com.fleetmatch.onboarding.dto;

import com.fleetmatch.company.documents.entity.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OnboardingDocumentRequirement {

    private DocumentType documentType;
    private String label;
    private boolean required;
}
