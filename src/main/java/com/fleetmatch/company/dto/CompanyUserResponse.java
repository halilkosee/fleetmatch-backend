package com.fleetmatch.company.dto;

import com.fleetmatch.user.entity.CompanyUserRole;
import com.fleetmatch.user.entity.UserStatus;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class CompanyUserResponse {

    private UUID id;

    private String firstName;

    private String lastName;

    private String email;

    private CompanyUserRole companyUserRole;

    private UserStatus status;
}
