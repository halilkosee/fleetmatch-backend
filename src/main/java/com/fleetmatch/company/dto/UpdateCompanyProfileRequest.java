package com.fleetmatch.company.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCompanyProfileRequest {

    @Size(max = 20)
    @Pattern(regexp = "^$|^MC-\\d{5,8}$", message = "Authority number must use format MC-123456")
    private String mcNumber;

    @Size(max = 100)
    private String dotNumber;

    @Size(max = 100)
    private String entityType;

    @Size(max = 50)
    @Pattern(regexp = "^$|^\\d{2}-\\d{7}$", message = "EIN must use format 12-3456789")
    private String ein;

    @Size(max = 100)
    private String stateOfFormation;

    @Size(max = 255)
    private String headquarters;

    @Size(max = 255)
    private String primaryContact;

    @Size(max = 100)
    private String authorityStatus;

    @Size(max = 100)
    private String brokerBondOrTrust;

    @Size(max = 100)
    private String insuranceCoverage;

    @Size(max = 1000)
    private String operatingRegions;

    @Size(max = 50)
    private String phone;

    @Size(max = 255)
    private String website;

    @Size(max = 255)
    private String dbaName;

    @Size(max = 255)
    private String email;

    private Integer fleetSize;

    @Size(max = 1000)
    private String description;
}
