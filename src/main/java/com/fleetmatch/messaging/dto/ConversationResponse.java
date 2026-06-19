package com.fleetmatch.messaging.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class ConversationResponse {

    private UUID id;
    private UUID loadId;

    private UUID brokerCompanyId;
    private String brokerCompanyName;

    private UUID fleetCompanyId;
    private String fleetCompanyName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
