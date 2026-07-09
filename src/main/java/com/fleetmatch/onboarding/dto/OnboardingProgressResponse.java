package com.fleetmatch.onboarding.dto;

import com.fleetmatch.company.entity.CompanyType;
import com.fleetmatch.company.entity.CompanyVerificationStatus;
import com.fleetmatch.company.documents.entity.DocumentType;
import com.fleetmatch.user.entity.UserStatus;
import lombok.Getter;

import java.util.List;

@Getter
public class OnboardingProgressResponse {

    private UserStatus userStatus;
    private CompanyType companyType;
    private CompanyVerificationStatus companyVerificationStatus;
    private boolean emailVerified;
    private boolean phoneVerified;
    private boolean companyInformationCompleted;
    private boolean documentsUploaded;
    private boolean marketSurveyCompleted;
    private boolean inReview;
    private boolean approved;
    private String estimatedReviewTime;
    private String message;
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
    private boolean submissionReady;

    public OnboardingProgressResponse(
            UserStatus userStatus,
            CompanyType companyType,
            CompanyVerificationStatus companyVerificationStatus,
            boolean emailVerified,
            boolean phoneVerified,
            boolean companyInformationCompleted,
            boolean documentsUploaded,
            boolean marketSurveyCompleted,
            boolean inReview,
            boolean approved,
            String estimatedReviewTime,
            String message,
            int completionPercentage,
            List<String> completedSections,
            List<String> incompleteSections,
            List<OnboardingSectionResponse> sections,
            List<OnboardingFieldRequirement> requiredFields,
            List<OnboardingDocumentRequirement> requiredDocuments,
            List<String> missingFields,
            List<DocumentType> missingDocuments,
            List<String> invalidFields,
            List<String> warnings,
            List<String> blockingErrors,
            boolean submissionReady
    ) {
        this.userStatus = userStatus;
        this.companyType = companyType;
        this.companyVerificationStatus = companyVerificationStatus;
        this.emailVerified = emailVerified;
        this.phoneVerified = phoneVerified;
        this.companyInformationCompleted = companyInformationCompleted;
        this.documentsUploaded = documentsUploaded;
        this.marketSurveyCompleted = marketSurveyCompleted;
        this.inReview = inReview;
        this.approved = approved;
        this.estimatedReviewTime = estimatedReviewTime;
        this.message = message;
        this.completionPercentage = completionPercentage;
        this.completedSections = completedSections;
        this.incompleteSections = incompleteSections;
        this.sections = sections;
        this.requiredFields = requiredFields;
        this.requiredDocuments = requiredDocuments;
        this.missingFields = missingFields;
        this.missingDocuments = missingDocuments;
        this.invalidFields = invalidFields;
        this.warnings = warnings;
        this.blockingErrors = blockingErrors;
        this.submissionReady = submissionReady;
    }
}
