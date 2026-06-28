package com.fleetmatch.admin.dto;

import com.fleetmatch.load.entity.EquipmentType;
import com.fleetmatch.load.entity.LoadStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class AdminLoadResponse {

    private UUID id;
    private LoadStatus status;

    private UUID brokerCompanyId;
    private String brokerCompanyName;
    private String brokerEmail;
    private String brokerPhone;

    private UUID createdByUserId;
    private String createdByName;
    private String createdByEmail;

    private long offerCount;

    private String pickupCity;
    private String pickupState;
    private LocalDate pickupDate;

    private String deliveryCity;
    private String deliveryState;
    private LocalDate deliveryDate;

    private EquipmentType equipmentType;

    private Integer weight;
    private Integer weightLbs;
    private BigDecimal rate;
    private Integer miles;

    private String commodity;
    private String referenceNumber;
    private String notes;
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

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
