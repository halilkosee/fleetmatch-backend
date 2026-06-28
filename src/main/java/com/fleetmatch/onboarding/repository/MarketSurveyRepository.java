package com.fleetmatch.onboarding.repository;

import com.fleetmatch.onboarding.entity.MarketSurvey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MarketSurveyRepository extends JpaRepository<MarketSurvey, UUID> {

    Optional<MarketSurvey> findByCompanyId(UUID companyId);

    boolean existsByCompanyId(UUID companyId);
}
