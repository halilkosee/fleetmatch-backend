package com.fleetmatch.company.review.repository;

import com.fleetmatch.company.review.entity.CompanyVerificationSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanyVerificationSnapshotRepository
        extends JpaRepository<CompanyVerificationSnapshot, UUID> {

    Optional<CompanyVerificationSnapshot> findTopByCompanyIdOrderByVersionNumberDesc(UUID companyId);

    List<CompanyVerificationSnapshot> findByCompanyIdOrderByVersionNumberDesc(UUID companyId);
}
