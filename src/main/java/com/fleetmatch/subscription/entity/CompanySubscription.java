package com.fleetmatch.subscription.entity;

import com.fleetmatch.common.entity.BaseEntity;
import com.fleetmatch.company.entity.Company;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "company_subscriptions")
public class CompanySubscription extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToOne(optional = false)
    @JoinColumn(name = "subscription_plan_id")
    private SubscriptionPlan subscriptionPlan;

    private LocalDate startDate;

    private LocalDate endDate;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false)
    private Boolean autoRenew = false;

    // OVERRIDES

    private BigDecimal customPrice;

    private Integer vehicleLimitOverride;

    private Integer userLimitOverride;

    private Integer monthlyLoadLimitOverride;

    private Integer loadLimitOverride;

    private Boolean canSubmitOffersOverride;

    private Boolean canViewContactInfoOverride;
}
