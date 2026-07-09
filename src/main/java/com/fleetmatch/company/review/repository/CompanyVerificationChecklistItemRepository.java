package com.fleetmatch.company.review.repository;

import com.fleetmatch.company.review.entity.CompanyVerificationChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanyVerificationChecklistItemRepository
        extends JpaRepository<CompanyVerificationChecklistItem, UUID> {

    List<CompanyVerificationChecklistItem> findBySnapshotIdOrderByItemKey(UUID snapshotId);

    Optional<CompanyVerificationChecklistItem> findBySnapshotIdAndItemKey(UUID snapshotId, String itemKey);

    long countBySnapshotIdAndMandatoryTrueAndCompletedFalse(UUID snapshotId);
}
