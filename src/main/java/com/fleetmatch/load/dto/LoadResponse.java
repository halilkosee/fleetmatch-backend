package com.fleetmatch.load.dto;

import com.fleetmatch.load.entity.EquipmentType;
import com.fleetmatch.load.entity.LoadStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
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

    private String brokerEmail;

    private String brokerPhone;

    private Integer weightLbs;
    private String description;

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

    private Integer palletCount;
    private Integer pieceCount;
    private Integer lengthInches;
    private Integer widthInches;
    private Integer heightInches;

    private boolean liftgateRequired;
    private boolean palletJackRequired;
    private boolean dockHighRequired;
    private boolean residentialDelivery;

    public LoadResponse(
            UUID id,
            String pickupCity,
            String pickupState,
            LocalDate pickupDate,
            String deliveryCity,
            String deliveryState,
            LocalDate deliveryDate,
            EquipmentType equipmentType,
            Integer weight,
            BigDecimal rate,
            Integer miles,
            String commodity,
            String referenceNumber,
            LoadStatus status,
            String notes,
            String brokerCompanyName,
            String brokerEmail,
            String brokerPhone
    ) {
        this.id = id;
        this.pickupCity = pickupCity;
        this.pickupState = pickupState;
        this.pickupDate = pickupDate;
        this.deliveryCity = deliveryCity;
        this.deliveryState = deliveryState;
        this.deliveryDate = deliveryDate;
        this.equipmentType = equipmentType;
        this.weight = weight;
        this.weightLbs = weight;
        this.rate = rate;
        this.miles = miles;
        this.commodity = commodity;
        this.referenceNumber = referenceNumber;
        this.status = status;
        this.notes = notes;
        this.brokerCompanyName = brokerCompanyName;
        this.brokerEmail = brokerEmail;
        this.brokerPhone = brokerPhone;
    }
}
