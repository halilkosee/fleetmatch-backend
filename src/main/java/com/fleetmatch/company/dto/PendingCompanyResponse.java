package com.fleetmatch.company.dto;

import com.fleetmatch.company.entity.CompanyType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class PendingCompanyResponse {

    private UUID id;
    private String legalName;
    private CompanyType type;
}