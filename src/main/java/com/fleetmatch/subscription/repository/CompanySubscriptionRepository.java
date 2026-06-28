package com.fleetmatch.subscription.repository;

import com.fleetmatch.subscription.entity.CompanySubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanySubscriptionRepository
        extends JpaRepository<CompanySubscription, UUID> {

    Optional<CompanySubscription> findByCompanyIdAndActiveTrue(
            UUID companyId
    );

    Optional<CompanySubscription> findTopByCompanyIdOrderByCreatedAtDesc(
            UUID companyId
    );

    List<CompanySubscription>
    findByActiveTrue();
}
