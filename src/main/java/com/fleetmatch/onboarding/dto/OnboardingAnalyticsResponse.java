package com.fleetmatch.onboarding.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class OnboardingAnalyticsResponse {

    private long brokerCompanies;
    private long fleetCompanies;
    private long inReviewCompanies;
    private long approvedCompanies;
    private long rejectedCompanies;
    private List<Metric> topOperatingStates;
    private List<Metric> topEquipmentTypes;
    private List<Metric> currentLoadBoards;
    private List<Metric> currentTms;

    @Getter
    @AllArgsConstructor
    public static class Metric {
        private String name;
        private long count;
    }
}
