package com.fleetmatch.load.dto;

import com.fleetmatch.load.entity.EquipmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CreateLoadRequest {

    @NotBlank
    private String pickupCity;

    @NotBlank
    private String pickupState;

    @NotBlank
    private String deliveryCity;

    @NotBlank
    private String deliveryState;

    @NotNull
    private EquipmentType equipmentType;

    @NotNull
    @Positive
    private Integer weight;

    @NotNull
    @Positive
    private BigDecimal rate;

    private String notes;

    @NotNull
    private LocalDate pickupDate;

    @NotNull
    private LocalDate deliveryDate;

    @NotNull
    @Positive
    private Integer miles;

    @NotBlank
    private String commodity;

    @NotBlank
    private String referenceNumber;
}