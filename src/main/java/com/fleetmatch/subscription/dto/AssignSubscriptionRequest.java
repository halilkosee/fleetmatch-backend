package com.fleetmatch.subscription.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class AssignSubscriptionRequest {

    private UUID companyId;

    private UUID subscriptionPlanId;

    private LocalDate startDate;

    private LocalDate endDate;

    private Boolean autoRenew;

    // OPTIONAL OVERRIDES

    private BigDecimal customPrice;

    private Integer vehicleLimitOverride;

    private Integer userLimitOverride;

    private Integer monthlyLoadLimitOverride;

    private Integer loadLimitOverride;

    private Boolean canSubmitOffersOverride;

    private Boolean canViewContactInfoOverride;
}
