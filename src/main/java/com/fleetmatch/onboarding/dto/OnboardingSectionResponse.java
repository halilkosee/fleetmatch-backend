package com.fleetmatch.onboarding.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class OnboardingSectionResponse {

    private String key;
    private String label;
    private int completionPercentage;
    private boolean complete;
    private List<String> completedItems;
    private List<String> missingItems;
    private List<String> warnings;
}
