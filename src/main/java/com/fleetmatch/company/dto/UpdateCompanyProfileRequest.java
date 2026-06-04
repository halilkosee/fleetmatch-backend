package com.fleetmatch.company.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCompanyProfileRequest {

    private String mcNumber;

    private String dotNumber;

    private String phone;

    private String website;
}
