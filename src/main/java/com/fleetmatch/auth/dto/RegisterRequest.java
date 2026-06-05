package com.fleetmatch.auth.dto;

import com.fleetmatch.company.entity.CompanyType;
import com.fleetmatch.user.entity.CompanyUserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank
    @Size(max = 255)
    private String companyLegalName;

    private String companyDbaName;

    @NotBlank
    @Email
    private String companyEmail;

    private String companyPhone;

    @NotNull
    private CompanyType companyType;

    @NotBlank
    @Size(max = 100)
    private String firstName;

    @NotBlank
    @Size(max = 100)
    private String lastName;

    @NotBlank
    @Email
    private String email;

    private String phone;

    @NotBlank
    @Size(min = 6, max = 100)
    private String password;

    private CompanyUserRole companyUserRole;
}