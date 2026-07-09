package com.fleetmatch.company.review.repository;

import com.fleetmatch.company.review.entity.CompanyVerificationSectionReview;
import com.fleetmatch.company.review.entity.SectionReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanyVerificationSectionReviewRepository
        extends JpaRepository<CompanyVerificationSectionReview, UUID> {

    List<CompanyVerificationSectionReview> findBySnapshotIdOrderBySectionKey(UUID snapshotId);

    Optional<CompanyVerificationSectionReview> findBySnapshotIdAndSectionKey(UUID snapshotId, String sectionKey);

    long countBySnapshotIdAndStatusNot(UUID snapshotId, SectionReviewStatus status);
}
