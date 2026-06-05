package com.fleetmatch.company.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCompanyProfileRequest {

    @Size(max = 100)
    private String mcNumber;

    @Size(max = 100)
    private String dotNumber;

    @Size(max = 50)
    private String phone;

    @Size(max = 255)
    private String website;
}