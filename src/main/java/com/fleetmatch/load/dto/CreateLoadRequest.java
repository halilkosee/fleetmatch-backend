package com.fleetmatch.load.dto;

import java.time.LocalDate;
import com.fleetmatch.load.entity.EquipmentType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateLoadRequest {

    private String pickupCity;
    private String pickupState;

    private String deliveryCity;
    private String deliveryState;

    private EquipmentType equipmentType;

    private Integer weight;

    private BigDecimal rate;

    private String notes;

    private LocalDate pickupDate;

    private LocalDate deliveryDate;

    private Integer miles;

    private String commodity;
    private String referenceNumber;
}