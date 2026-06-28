package com.fleetmatch.admin.dto;

import com.fleetmatch.company.entity.CompanyType;
import com.fleetmatch.company.entity.CompanyVerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class AdminApprovalQueueItemResponse {

    private UUID companyId;
    private String legalName;
    private CompanyType companyType;
    private CompanyVerificationStatus verificationStatus;
    private Integer fleetSize;
    private List<String> operatingStates;
    private List<String> equipmentTypes;
    private Integer averageLoadsPerWeek;
    private Integer manualPriority;
    private LocalDateTime registrationDate;
    private boolean documentsUploaded;
    private boolean surveyCompleted;
    private String verificationNotes;
}
