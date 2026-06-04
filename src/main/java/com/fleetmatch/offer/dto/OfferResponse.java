package com.fleetmatch.offer.dto;

import com.fleetmatch.offer.entity.OfferStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class OfferResponse {

    private UUID id;
    private UUID loadId;

    private UUID carrierUserId;
    private String carrierName;

    private BigDecimal amount;
    private String message;

    private OfferStatus status;
}