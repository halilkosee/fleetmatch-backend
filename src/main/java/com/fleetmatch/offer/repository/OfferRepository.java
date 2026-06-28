package com.fleetmatch.offer.repository;

import com.fleetmatch.offer.entity.Offer;
import com.fleetmatch.offer.entity.OfferStatus;
import com.fleetmatch.load.entity.LoadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OfferRepository extends JpaRepository<Offer, UUID> {

    List<Offer> findByLoadId(UUID loadId);

    long countByLoadId(UUID loadId);

    List<Offer> findByLoadIdAndStatus(
            UUID loadId,
            OfferStatus status
    );

    Optional<Offer> findFirstByLoadIdAndStatus(
            UUID loadId,
            OfferStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Offer o join fetch o.load where o.id = :offerId")
    Optional<Offer> findByIdWithLoadForUpdate(UUID offerId);

    boolean existsByLoadIdAndFleetUserId(
            UUID loadId,
            UUID FleetUserId
    );

    long countByStatus(OfferStatus status);

    Page<Offer> findByStatus(OfferStatus status, Pageable pageable);

    List<Offer> findByFleetUserCompanyId(UUID companyId);

    Page<Offer> findByFleetUserCompanyId(
            UUID companyId,
            Pageable pageable
    );

    List<Offer> findByFleetUserCompanyIdAndStatus(
            UUID companyId,
            OfferStatus status
    );

    long countByFleetUserCompanyId(UUID companyId);

    long countByFleetUserCompanyIdAndStatus(UUID companyId, OfferStatus status);

    @Query("""
            select count(o)
            from Offer o
            where o.load.brokerCompany.id = :brokerCompanyId
            """)
    long countByBrokerCompanyId(UUID brokerCompanyId);

    @Query("""
            select count(o)
            from Offer o
            where o.load.brokerCompany.id = :brokerCompanyId
              and o.status = :status
            """)
    long countByBrokerCompanyIdAndStatus(UUID brokerCompanyId, OfferStatus status);

    @Query("""
            select count(o)
            from Offer o
            where o.fleetUser.company.id = :companyId
              and o.status = 'CONFIRMED'
              and o.load.status = :loadStatus
            """)
    long countConfirmedFleetLoadsByStatus(UUID companyId, LoadStatus loadStatus);
}
