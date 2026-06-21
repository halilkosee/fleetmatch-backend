package com.fleetmatch.subscription.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class CompanySubscriptionResponse {

    private UUID id;

    private UUID companyId;

    private String companyName;

    private UUID planId;

    private String planName;

    private LocalDate startDate;

    private LocalDate endDate;

    private Boolean active;

    private BigDecimal customPrice;

    private Integer vehicleLimitOverride;

    private Integer userLimitOverride;

    private Integer monthlyLoadLimitOverride;

    private Integer loadLimitOverride;
}
