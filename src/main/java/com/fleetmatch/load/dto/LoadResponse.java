package com.fleetmatch.load.dto;

import com.fleetmatch.load.entity.EquipmentType;
import com.fleetmatch.load.entity.LoadStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class LoadResponse {

    private UUID id;

    private String pickupCity;
    private String pickupState;
    private LocalDate pickupDate;

    private String deliveryCity;
    private String deliveryState;
    private LocalDate deliveryDate;

    private EquipmentType equipmentType;

    private Integer weight;
    private BigDecimal rate;
    private Integer miles;

    private String commodity;
    private String referenceNumber;

    private LoadStatus status;
    private String notes;

    private String brokerCompanyName;
}