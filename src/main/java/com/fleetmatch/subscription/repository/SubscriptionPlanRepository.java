package com.fleetmatch.subscription.repository;

import com.fleetmatch.subscription.entity.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SubscriptionPlanRepository
        extends JpaRepository<SubscriptionPlan, UUID> {

    Optional<SubscriptionPlan> findByName(String name);
}