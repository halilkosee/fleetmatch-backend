package com.fleetmatch.onboarding.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MarketSurveyRequest {

    @Size(max = 100)
    private List<String> operatingStates;

    @Size(max = 100)
    private List<String> equipmentTypes;

    @Min(0)
    @Max(100000)
    private Integer averageLoadsPerWeek;

    @Min(0)
    @Max(100000)
    private Integer fleetSize;

    @Size(max = 255)
    private String currentLoadBoard;

    @Size(max = 255)
    private String currentTms;

    private Boolean futureIntegrationInterest;

    @Size(max = 4000)
    private String biggestOperationalChallenges;

    @Size(max = 50)
    private String homeState;

    @Size(max = 100)
    private List<String> preferredRegions;

    @Min(0)
    @Max(100000)
    private Integer preferredMileage;

    private Boolean dedicatedRouteInterest;
}
