package com.fleetmatch.admin.dto;

import com.fleetmatch.offer.entity.OfferStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class AdminOfferResponse {

    private UUID id;
    private UUID loadId;
    private OfferStatus status;
    private BigDecimal amount;
    private String message;
    private UUID fleetCompanyId;
    private String fleetCompanyName;
    private UUID fleetUserId;
    private String fleetUserName;
    private String fleetUserEmail;
    private UUID brokerCompanyId;
    private String brokerCompanyName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
