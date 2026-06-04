package com.fleetmatch.offer.entity;

import com.fleetmatch.common.entity.BaseEntity;
import com.fleetmatch.load.entity.Load;
import com.fleetmatch.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "offers")
public class Offer extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "load_id", nullable = false)
    private Load load;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carrier_user_id", nullable = false)
    private User carrierUser;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OfferStatus status = OfferStatus.PENDING;
}