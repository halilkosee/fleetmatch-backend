package com.fleetmatch.vehicle.dto;

import com.fleetmatch.vehicle.entity.VehicleCapability;
import com.fleetmatch.vehicle.entity.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class CreateVehicleRequest {

    @NotBlank
    private String plateNumber;

    private String vinNumber;

    @NotNull
    private VehicleType type;

    @NotNull
    private Integer lengthFeet;

    private Set<VehicleCapability> capabilities;

    private String make;

    private String model;

    private Integer year;
}