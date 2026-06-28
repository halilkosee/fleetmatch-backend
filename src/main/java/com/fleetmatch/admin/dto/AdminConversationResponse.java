package com.fleetmatch.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class AdminConversationResponse {

    private UUID id;
    private UUID loadId;
    private UUID brokerCompanyId;
    private String brokerCompanyName;
    private UUID fleetCompanyId;
    private String fleetCompanyName;
    private boolean archived;
    private LocalDateTime archivedAt;
    private long messageCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
