package com.fleetmatch.offer.repository;

import com.fleetmatch.offer.entity.Offer;
import com.fleetmatch.offer.entity.OfferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OfferRepository extends JpaRepository<Offer, UUID> {

    @Override
    @EntityGraph(attributePaths = {
            "load",
            "load.brokerCompany",
            "fleetUser",
            "fleetUser.company"
    })
    Optional<Offer> findById(UUID id);

    @EntityGraph(attributePaths = {
            "load",
            "load.brokerCompany",
            "fleetUser",
            "fleetUser.company"
    })
    List<Offer> findByLoadId(UUID loadId);

    @EntityGraph(attributePaths = {
            "load",
            "load.brokerCompany",
            "fleetUser",
            "fleetUser.company"
    })
    List<Offer> findByLoadIdAndStatus(
            UUID loadId,
            OfferStatus status
    );

    @EntityGraph(attributePaths = {
            "load",
            "load.brokerCompany",
            "fleetUser",
            "fleetUser.company"
    })
    Optional<Offer> findFirstByLoadIdAndStatus(
            UUID loadId,
            OfferStatus status
    );

    boolean existsByLoadIdAndFleetUserId(
            UUID loadId,
            UUID FleetUserId
    );

    long countByStatus(OfferStatus status);

    @EntityGraph(attributePaths = {
            "load",
            "load.brokerCompany",
            "fleetUser",
            "fleetUser.company"
    })
    List<Offer> findByFleetUserCompanyId(UUID companyId);

    @EntityGraph(attributePaths = {
            "load",
            "load.brokerCompany",
            "fleetUser",
            "fleetUser.company"
    })
    Page<Offer> findByFleetUserCompanyId(
            UUID companyId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "load",
            "load.brokerCompany",
            "fleetUser",
            "fleetUser.company"
    })
    List<Offer> findByFleetUserCompanyIdAndStatus(
            UUID companyId,
            OfferStatus status
    );
}
