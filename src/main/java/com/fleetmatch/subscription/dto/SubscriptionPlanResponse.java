package com.fleetmatch.subscription.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class SubscriptionPlanResponse {

    private UUID id;

    private String name;

    private String description;

    private BigDecimal monthlyPrice;

    private Integer maxVehicles;

    private Integer maxUsers;

    private Integer maxLoadsPerMonth;

    private Integer maxLoadsVisible;

    private Boolean canSubmitOffers;

    private Boolean canViewContactInfo;

    private Boolean active;
}
