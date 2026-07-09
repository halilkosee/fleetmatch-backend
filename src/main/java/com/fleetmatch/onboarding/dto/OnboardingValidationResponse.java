package com.fleetmatch.onboarding.dto;

import com.fleetmatch.company.documents.entity.DocumentType;
import com.fleetmatch.company.entity.CompanyType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class OnboardingValidationResponse {

    private CompanyType companyType;
    private boolean submissionReady;
    private int completionPercentage;
    private List<String> completedSections;
    private List<String> incompleteSections;
    private List<OnboardingSectionResponse> sections;
    private List<OnboardingFieldRequirement> requiredFields;
    private List<OnboardingDocumentRequirement> requiredDocuments;
    private List<String> missingFields;
    private List<DocumentType> missingDocuments;
    private List<String> invalidFields;
    private List<String> warnings;
    private List<String> blockingErrors;
    private String estimatedReviewTime;
}
