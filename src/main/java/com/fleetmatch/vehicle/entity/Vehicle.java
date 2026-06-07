package com.fleetmatch.vehicle.entity;

import com.fleetmatch.common.entity.BaseEntity;
import com.fleetmatch.company.entity.Company;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "vehicles")
public class Vehicle extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    // IDENTIFICATION

    @Column(nullable = false, unique = true)
    private String plateNumber;

    @Column(unique = true)
    private String vinNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleType type;

    @Column(nullable = false)
    private Integer lengthFeet;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "vehicle_capabilities",
            joinColumns = @JoinColumn(name = "vehicle_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "capability")
    private Set<VehicleCapability> capabilities =
            new HashSet<>();

    private String make;

    private String model;

    private Integer year;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleStatus status =
            VehicleStatus.AVAILABLE;

    @Column(nullable = false)
    private Boolean active = true;
}