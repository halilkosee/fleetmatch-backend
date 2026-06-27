package com.fleetmatch.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyEmailChangeRequest {

    @NotBlank
    @Email
    private String newEmail;

    @NotBlank
    @Pattern(regexp = "\\d{6}")
    private String code;
}
