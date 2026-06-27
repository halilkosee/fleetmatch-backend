package com.fleetmatch.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResendPhoneCodeRequest {

    @NotBlank
    private String phone;
}
