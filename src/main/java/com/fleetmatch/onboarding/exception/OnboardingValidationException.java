package com.fleetmatch.onboarding.exception;

import com.fleetmatch.onboarding.dto.OnboardingValidationResponse;
import lombok.Getter;

@Getter
public class OnboardingValidationException extends RuntimeException {

    private final OnboardingValidationResponse validation;

    public OnboardingValidationException(OnboardingValidationResponse validation) {
        super("Onboarding application is incomplete");
        this.validation = validation;
    }
}
