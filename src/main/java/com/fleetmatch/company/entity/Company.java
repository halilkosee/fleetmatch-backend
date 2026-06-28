package com.fleetmatch.company.entity;

import com.fleetmatch.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "companies")
public class Company extends BaseEntity {

    // COMPANY INFO

    @Column(nullable = false)
    private String legalName;

    private String dbaName;

    @Column(length = 100)
    private String entityType;

    @Column(length = 50)
    private String ein;

    @Column(length = 100)
    private String stateOfFormation;

    @Column(length = 255)
    private String headquarters;

    @Column(nullable = false, unique = true)
    private String email;

    private String phone;

    @Column(length = 255)
    private String primaryContact;

    @Column(length = 255)
    private String website;

    // COMPANY CLASSIFICATION

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompanyType type;

    private Integer fleetSize;

    @Column(length = 1000)
    private String description;

    // VERIFICATION

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompanyVerificationStatus verificationStatus =
            CompanyVerificationStatus.PENDING;

    @Column(length = 100)
    private String mcNumber;

    @Column(length = 100)
    private String dotNumber;

    @Column(length = 100)
    private String authorityStatus;

    @Column(length = 100)
    private String brokerBondOrTrust;

    @Column(length = 100)
    private String insuranceCoverage;

    @Column(length = 1000)
    private String operatingRegions;

    @Column(length = 2000)
    private String verificationNotes;

    @Column(length = 2000)
    private String rejectionReason;

    @Column(length = 2000)
    private String additionalDocumentsRequest;

    @Column(nullable = false)
    private boolean companyInformationCompleted;

    @Column(nullable = false)
    private boolean marketSurveyCompleted;

    private Integer manualPriority;

    @Column(length = 1000)
    private String adminInternalNotes;
}
