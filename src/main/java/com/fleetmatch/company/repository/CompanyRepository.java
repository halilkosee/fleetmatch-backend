package com.fleetmatch.company.repository;

import com.fleetmatch.company.entity.Company;
import com.fleetmatch.company.entity.CompanyType;
import com.fleetmatch.company.entity.CompanyVerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {
    long countByType(CompanyType type);
    long countByVerificationStatus(
            CompanyVerificationStatus verificationStatus
    );

    Optional<Company> findById(UUID id);

    List<Company> findByVerificationStatus(
            CompanyVerificationStatus verificationStatus
    );

    List<Company> findByVerificationStatusIn(
            Collection<CompanyVerificationStatus> verificationStatuses
    );
}
