package com.fleetmatch.company.repository;

import com.fleetmatch.company.entity.Company;
import com.fleetmatch.company.entity.CompanyType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {
    long countByType(CompanyType type);
}