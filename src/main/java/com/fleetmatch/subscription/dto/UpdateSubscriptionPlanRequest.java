package com.fleetmatch.subscription.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UpdateSubscriptionPlanRequest {

    private String name;

    private String description;

    private BigDecimal monthlyPrice;

    private Integer maxVehicles;

    private Integer maxUsers;

    private Integer maxLoadsVisible;

    private Boolean canSubmitOffers;

    private Boolean canViewContactInfo;

    private Boolean active;
}