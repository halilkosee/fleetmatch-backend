package com.fleetmatch.company.dto;

import com.fleetmatch.company.entity.CompanyType;
import com.fleetmatch.company.entity.CompanyVerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class CompanyProfileResponse {

    private UUID id;

    private String legalName;

    private CompanyType type;

    private CompanyVerificationStatus verificationStatus;

    private String mcNumber;

    private String dotNumber;

    private String entityType;

    private String ein;

    private String stateOfFormation;

    private String headquarters;

    private String normalizedHeadquarters;

    private boolean headquartersAddressVerified;

    private String headquartersAddressVerificationStatus;

    private Double headquartersLatitude;

    private Double headquartersLongitude;

    private String primaryContact;

    private String authorityStatus;

    private String brokerBondOrTrust;

    private String insuranceCoverage;

    private String operatingRegions;

    private String phone;

    private String website;

    private String dbaName;

    private String email;

    private Integer fleetSize;

    private String description;

    private boolean companyInformationCompleted;

    private boolean marketSurveyCompleted;
}
