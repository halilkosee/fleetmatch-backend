package com.fleetmatch.vehicle.dto;

import com.fleetmatch.vehicle.entity.VehicleCapability;
import com.fleetmatch.vehicle.entity.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class VehicleResponse {

    private UUID id;

    private String plateNumber;

    private String vinNumber;

    private VehicleType type;

    private Integer lengthFeet;

    private Set<VehicleCapability> capabilities;

    private String make;

    private String model;

    private Integer year;

    private Boolean active;
}