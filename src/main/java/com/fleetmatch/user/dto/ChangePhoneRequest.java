package com.fleetmatch.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePhoneRequest {

    @NotBlank
    private String newPhone;
}
