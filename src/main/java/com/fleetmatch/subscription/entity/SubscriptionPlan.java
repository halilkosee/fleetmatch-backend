package com.fleetmatch.subscription.entity;

import com.fleetmatch.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "subscription_plans")
public class SubscriptionPlan extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private BigDecimal monthlyPrice;


    private Integer maxVehicles;


    private Integer maxUsers;


    private Integer maxLoadsPerMonth;


    private Integer maxLoadsVisible;

    @Column(nullable = false)
    private Boolean canSubmitOffers;

    @Column(nullable = false)
    private Boolean canViewContactInfo;

    @Column(nullable = false)
    private Boolean active = true;
}
