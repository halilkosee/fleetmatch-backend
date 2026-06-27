package com.fleetmatch.company.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCompanySettingsRequest {

    @Size(max = 255)
    private String dbaName;

    @Size(max = 255)
    private String website;

    @Email
    private String companyEmail;

    @Size(max = 50)
    private String companyPhone;

    private Integer fleetSize;

    @Size(max = 1000)
    private String description;
}
