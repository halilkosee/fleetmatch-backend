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

    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private long unreadCount;

    public ConversationResponse(
            UUID id,
            UUID loadId,
            UUID brokerCompanyId,
            String brokerCompanyName,
            UUID fleetCompanyId,
            String fleetCompanyName,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.loadId = loadId;
        this.brokerCompanyId = brokerCompanyId;
        this.brokerCompanyName = brokerCompanyName;
        this.fleetCompanyId = fleetCompanyId;
        this.fleetCompanyName = fleetCompanyName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
