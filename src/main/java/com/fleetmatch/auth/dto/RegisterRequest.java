package com.fleetmatch.auth.dto;

import com.fleetmatch.company.entity.CompanyType;
import com.fleetmatch.user.entity.CompanyUserRole;
import com.fleetmatch.user.entity.PlatformRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank
    private String companyLegalName;

    private String companyDbaName;

    @NotBlank
    @Email
    private String companyEmail;

    private String companyPhone;

    @NotNull
    private CompanyType companyType;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotBlank
    @Email
    private String email;

    private String phone;

    @NotBlank
    private String password;

    @NotNull
    private PlatformRole platformRole;

    private CompanyUserRole companyUserRole;
}