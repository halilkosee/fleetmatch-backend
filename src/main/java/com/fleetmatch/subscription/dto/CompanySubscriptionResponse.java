package com.fleetmatch.subscription.dto;

import com.fleetmatch.subscription.entity.SubscriptionPaymentStatus;
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

    private SubscriptionPaymentStatus paymentStatus;

    private String paymentProvider;

    private String externalSubscriptionId;

    private BigDecimal customPrice;

    private Integer vehicleLimitOverride;

    private Integer userLimitOverride;

    private Integer monthlyLoadLimitOverride;

    private Integer loadLimitOverride;
}
