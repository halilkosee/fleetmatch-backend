package com.fleetmatch.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class VerificationCodeResponse {

    private String message;

    private String debugCode;
}
