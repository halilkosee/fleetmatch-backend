package com.fleetmatch.vehicle.dto;

import com.fleetmatch.vehicle.entity.VehicleCapability;
import com.fleetmatch.vehicle.entity.VehicleType;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class UpdateVehicleRequest {

    private String plateNumber;

    private String vinNumber;

    private VehicleType type;

    private Integer lengthFeet;

    private Set<VehicleCapability> capabilities;

    private String make;

    private String model;

    private Integer year;
}