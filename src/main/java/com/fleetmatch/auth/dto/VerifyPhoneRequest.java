package com.fleetmatch.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyPhoneRequest {

    @NotBlank
    private String phone;

    @NotBlank
    @Pattern(regexp = "\\d{6}")
    private String code;
}
