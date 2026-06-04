package com.fleetmatch.offer.repository;

import com.fleetmatch.offer.entity.Offer;
import com.fleetmatch.offer.entity.OfferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OfferRepository extends JpaRepository<Offer, UUID> {

    List<Offer> findByLoadId(UUID loadId);

    List<Offer> findByLoadIdAndStatus(
            UUID loadId,
            OfferStatus status
    );

    Optional<Offer> findFirstByLoadIdAndStatus(
            UUID loadId,
            OfferStatus status
    );

    boolean existsByLoadIdAndCarrierUserId(
            UUID loadId,
            UUID carrierUserId
    );

    long countByStatus(OfferStatus status);

    List<Offer> findByCarrierUserCompanyId(UUID companyId);

    Page<Offer> findByCarrierUserCompanyId(
            UUID companyId,
            Pageable pageable
    );

    List<Offer> findByCarrierUserCompanyIdAndStatus(
            UUID companyId,
            OfferStatus status
    );
}