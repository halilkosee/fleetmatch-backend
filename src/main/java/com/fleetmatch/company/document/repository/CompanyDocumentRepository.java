package com.fleetmatch.company.document.repository;

import com.fleetmatch.company.document.entity.CompanyDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CompanyDocumentRepository
        extends JpaRepository<CompanyDocument, UUID> {

    List<CompanyDocument> findByCompanyId(UUID companyId);
}