package com.fleetmatch.onboarding.dto;

import com.fleetmatch.company.documents.dto.CompanyDocumentResponse;
import com.fleetmatch.company.entity.CompanyType;
import com.fleetmatch.company.entity.CompanyVerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class OnboardingPreviewResponse {

    private CompanyPreview company;
    private ContactPreview contact;
    private List<CompanyDocumentResponse> documents;
    private SurveyPreview survey;
    private OnboardingValidationResponse validation;

    @Getter
    @AllArgsConstructor
    public static class CompanyPreview {
        private UUID id;
        private String legalName;
        private String dbaName;
        private CompanyType type;
        private CompanyVerificationStatus verificationStatus;
        private String entityType;
        private String ein;
        private String stateOfFormation;
        private String headquarters;
        private String mcNumber;
        private String dotNumber;
        private String authorityStatus;
        private String brokerBondOrTrust;
        private String insuranceCoverage;
        private String operatingRegions;
        private Integer fleetSize;
        private long activeVehicleCount;
    }

    @Getter
    @AllArgsConstructor
    public static class ContactPreview {
        private String primaryContact;
        private String email;
        private String phone;
        private String website;
        private boolean emailVerified;
        private boolean phoneVerified;
    }

    @Getter
    @AllArgsConstructor
    public static class SurveyPreview {
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
