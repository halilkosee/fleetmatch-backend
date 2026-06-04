package com.fleetmatch.company.dto;

import com.fleetmatch.company.entity.CompanyType;
import com.fleetmatch.company.entity.CompanyVerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class CompanyResponse {

    private UUID id;

    private String legalName;

    private CompanyType type;

    private CompanyVerificationStatus verificationStatus;
}