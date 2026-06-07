package com.fleetmatch.vehicle.repository;

import com.fleetmatch.vehicle.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VehicleRepository
        extends JpaRepository<Vehicle, UUID> {

    List<Vehicle> findByCompanyId(UUID companyId);

    List<Vehicle> findByCompanyIdAndActiveTrue(UUID companyId);

    long countByCompanyIdAndActiveTrue(UUID companyId);

    boolean existsByPlateNumber(String plateNumber);

    boolean existsByVinNumber(String vinNumber);
}