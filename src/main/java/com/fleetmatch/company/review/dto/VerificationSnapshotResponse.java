package com.fleetmatch.company.review.dto;

import com.fleetmatch.company.entity.CompanyType;
import com.fleetmatch.company.entity.CompanyVerificationStatus;
import com.fleetmatch.user.entity.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class VerificationSnapshotResponse {

    private UUID id;
    private int versionNumber;
    private CompanyType companyType;
    private UserStatus userStatus;
    private CompanyVerificationStatus companyVerificationStatus;
    private LocalDateTime submittedAt;
    private int completionPercentage;
    private boolean submissionReady;
    private List<String> completedSections;
    private List<String> incompleteSections;
    private List<String> missingFields;
    private List<String> missingDocuments;
    private List<String> invalidFields;
    private List<String> warnings;
    private List<String> blockingErrors;
    private String legalName;
    private String email;
    private String phone;
    private String mcNumber;
    private String dotNumber;
    private String ein;
    private String headquarters;
    private String website;
    private Integer fleetSize;
    private long activeVehicleCount;
}
