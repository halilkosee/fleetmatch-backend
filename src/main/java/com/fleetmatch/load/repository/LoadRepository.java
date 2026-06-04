package com.fleetmatch.load.repository;

import com.fleetmatch.load.entity.EquipmentType;
import com.fleetmatch.load.entity.Load;
import com.fleetmatch.load.entity.LoadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface LoadRepository extends JpaRepository<Load, UUID> {

    List<Load> findByStatus(LoadStatus status);

    Page<Load> findByStatus(LoadStatus status, Pageable pageable);

    List<Load> findByStatusAndPickupStateIgnoreCaseAndDeliveryStateIgnoreCaseAndEquipmentType(
            LoadStatus status,
            String pickupState,
            String deliveryState,
            EquipmentType equipmentType
    );

    List<Load> findByStatusAndPickupStateIgnoreCase(
            LoadStatus status,
            String pickupState
    );

    List<Load> findByStatusAndDeliveryStateIgnoreCase(
            LoadStatus status,
            String deliveryState
    );

    List<Load> findByStatusAndEquipmentType(
            LoadStatus status,
            EquipmentType equipmentType
    );

    long countByStatus(LoadStatus status);

    Page<Load> findByBrokerCompanyId(UUID brokerCompanyId, Pageable pageable);

}