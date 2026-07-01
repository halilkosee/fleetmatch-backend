package com.fleetmatch.company.documents.repository;

import com.fleetmatch.company.documents.entity.CompanyDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CompanyDocumentRepository
        extends JpaRepository<CompanyDocument, UUID> {

    List<CompanyDocument> findByCompanyId(UUID companyId);

    boolean existsByCompanyId(UUID companyId);
}
