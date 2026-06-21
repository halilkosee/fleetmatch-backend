package com.fleetmatch.load.repository;

import com.fleetmatch.load.entity.EquipmentType;
import com.fleetmatch.load.entity.Load;
import com.fleetmatch.load.entity.LoadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadRepository extends JpaRepository<Load, UUID> {

    @Override
    @EntityGraph(attributePaths = {
            "brokerCompany",
            "createdBy"
    })
    Optional<Load> findById(UUID id);

    @EntityGraph(attributePaths = {
            "brokerCompany",
            "createdBy"
    })
    List<Load> findByStatus(LoadStatus status);

    @EntityGraph(attributePaths = {
            "brokerCompany",
            "createdBy"
    })
    Page<Load> findByStatus(LoadStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {
            "brokerCompany",
            "createdBy"
    })
    List<Load> findByStatusAndPickupStateIgnoreCaseAndDeliveryStateIgnoreCaseAndEquipmentType(
            LoadStatus status,
            String pickupState,
            String deliveryState,
            EquipmentType equipmentType
    );

    @EntityGraph(attributePaths = {
            "brokerCompany",
            "createdBy"
    })
    List<Load> findByStatusAndPickupStateIgnoreCase(
            LoadStatus status,
            String pickupState
    );

    @EntityGraph(attributePaths = {
            "brokerCompany",
            "createdBy"
    })
    List<Load> findByStatusAndDeliveryStateIgnoreCase(
            LoadStatus status,
            String deliveryState
    );

    @EntityGraph(attributePaths = {
            "brokerCompany",
            "createdBy"
    })
    List<Load> findByStatusAndEquipmentType(
            LoadStatus status,
            EquipmentType equipmentType
    );

    long countByStatus(LoadStatus status);

    @EntityGraph(attributePaths = {
            "brokerCompany",
            "createdBy"
    })
    Page<Load> findByBrokerCompanyId(UUID brokerCompanyId, Pageable pageable);

}
