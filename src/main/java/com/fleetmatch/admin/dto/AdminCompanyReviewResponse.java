package com.fleetmatch.admin.dto;

import com.fleetmatch.company.documents.dto.CompanyDocumentResponse;
import com.fleetmatch.company.dto.CompanyProfileResponse;
import com.fleetmatch.company.entity.CompanyType;
import com.fleetmatch.company.entity.CompanyVerificationStatus;
import com.fleetmatch.company.review.dto.CompanyReviewEventResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class AdminCompanyReviewResponse {

    private UUID companyId;
    private CompanyProfileResponse company;
    private CompanyVerificationStatus verificationStatus;
    private String verificationNotes;
    private String rejectionReason;
    private String additionalDocumentsRequest;
    private String adminInternalNotes;
    private Integer manualPriority;
    private LocalDateTime registrationDate;
    private List<CompanyDocumentResponse> documents;
    private List<CompanyReviewEventResponse> reviewHistory;
    private Survey survey;

    @Getter
    @AllArgsConstructor
    public static class Survey {
        private CompanyType companyType;
        private List<String> operatingStates;
        private List<String> equipmentTypes;
        private Integer averageLoadsPerWeek;
        private Integer fleetSize;
        private String currentLoadBoard;
        private String currentTms;
        private Boolean futureIntegrationInterest;
        private String biggestOperationalChallenges;
        private String homeState;
        private List<String> preferredRegions;
        private Integer preferredMileage;
        private Boolean dedicatedRouteInterest;
    }
}
