package com.fleetmatch.load.entity;

import com.fleetmatch.common.entity.BaseEntity;
import com.fleetmatch.company.entity.Company;
import com.fleetmatch.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@Entity
@Table(name = "loads")
public class Load extends BaseEntity {

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "broker_company_id", nullable = false)
    private Company brokerCompany;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;

    @Column(nullable = false)
    private String pickupCity;

    @Column(nullable = false)
    private String pickupState;

    @Column(nullable = false)
    private LocalDate pickupDate;

    @Column(nullable = false)
    private String deliveryCity;

    @Column(nullable = false)
    private String deliveryState;

    private LocalDate deliveryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EquipmentType equipmentType;

    private Integer weight;

    private Integer weightLbs;

    @Column(nullable = false)
    private BigDecimal rate;

    private Integer miles;

    private String commodity;

    private String referenceNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoadStatus status = LoadStatus.POSTED;

    @Column(length = 1000)
    private String notes;

    @Column(length = 1000)
    private String description;

    private LocalDateTime offerDeadlineAt;

    private LocalDateTime confirmationDeadlineAt;

    private LocalDateTime expiredAt;

    private String pickupStreetAddress;
    private String pickupZipCode;
    private String pickupLocationName;
    private String pickupContactName;
    private String pickupContactPhone;
    private LocalTime pickupTimeWindowStart;
    private LocalTime pickupTimeWindowEnd;

    @Column(length = 1000)
    private String pickupInstructions;

    private String deliveryStreetAddress;
    private String deliveryZipCode;
    private String deliveryLocationName;
    private String deliveryContactName;
    private String deliveryContactPhone;
    private LocalTime deliveryTimeWindowStart;
    private LocalTime deliveryTimeWindowEnd;

    @Column(length = 1000)
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
}
