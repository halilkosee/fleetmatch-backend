package com.fleetmatch.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyPhoneChangeRequest {

    @NotBlank
    private String newPhone;

    @NotBlank
    @Pattern(regexp = "\\d{6}")
    private String code;
}
