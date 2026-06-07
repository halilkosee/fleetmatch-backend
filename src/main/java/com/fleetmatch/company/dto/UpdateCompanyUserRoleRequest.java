package com.fleetmatch.company.dto;

import com.fleetmatch.user.entity.CompanyUserRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateCompanyUserRoleRequest {

    @NotNull
    private CompanyUserRole companyUserRole;
}