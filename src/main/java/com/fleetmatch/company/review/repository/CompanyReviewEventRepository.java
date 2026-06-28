package com.fleetmatch.company.review.repository;

import com.fleetmatch.company.review.entity.CompanyReviewEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CompanyReviewEventRepository extends JpaRepository<CompanyReviewEvent, UUID> {

    List<CompanyReviewEvent> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);
}
