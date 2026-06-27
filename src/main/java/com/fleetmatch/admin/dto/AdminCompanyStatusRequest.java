package com.fleetmatch.admin.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminCompanyStatusRequest {

    @Size(max = 2000)
    private String notes;
}
