package com.fleetmatch.company.review.repository;

import com.fleetmatch.company.review.entity.CompanyVerificationRiskAssessment;
import com.fleetmatch.company.review.entity.VerificationRiskLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CompanyVerificationRiskAssessmentRepository
        extends JpaRepository<CompanyVerificationRiskAssessment, UUID> {

    Optional<CompanyVerificationRiskAssessment> findTopByCompanyIdOrderByCreatedAtDesc(UUID companyId);

    long countByLevel(VerificationRiskLevel level);
}
