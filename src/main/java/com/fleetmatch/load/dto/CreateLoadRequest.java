package com.fleetmatch.load.dto;

import com.fleetmatch.load.entity.EquipmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

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

    @Positive
    private Integer weight;

    @Positive
    private Integer weightLbs;

    @NotNull
    @Positive
    private BigDecimal rate;

    private String notes;

    @NotBlank
    @Size(max = 1000)
    private String description;

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

    private String pickupStreetAddress;
    private String pickupZipCode;
    private String pickupLocationName;
    private String pickupContactName;
    private String pickupContactPhone;
    private LocalTime pickupTimeWindowStart;
    private LocalTime pickupTimeWindowEnd;
    private String pickupInstructions;

    private String deliveryStreetAddress;
    private String deliveryZipCode;
    private String deliveryLocationName;
    private String deliveryContactName;
    private String deliveryContactPhone;
    private LocalTime deliveryTimeWindowStart;
    private LocalTime deliveryTimeWindowEnd;
    private String deliveryInstructions;

    @Positive
    private Integer palletCount;

    @Positive
    private Integer pieceCount;

    @Positive
    private Integer lengthInches;

    @Positive
    private Integer widthInches;

    @Positive
    private Integer heightInches;

    private boolean liftgateRequired;
    private boolean palletJackRequired;
    private boolean dockHighRequired;
    private boolean residentialDelivery;
}
