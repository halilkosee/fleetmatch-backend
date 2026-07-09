package com.fleetmatch.onboarding.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OnboardingFieldRequirement {

    private String key;
    private String label;
    private boolean required;
}
