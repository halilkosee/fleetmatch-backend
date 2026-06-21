package com.fleetmatch.subscription.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateSubscriptionPlanRequest {

    private String name;

    private String description;

    private BigDecimal monthlyPrice;

    private Integer maxVehicles;

    private Integer maxUsers;

    private Integer maxLoadsPerMonth;

    private Integer maxLoadsVisible;

    private Boolean canSubmitOffers;

    private Boolean canViewContactInfo;
}
