package com.fleetmatch.messaging.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class MessageResponse {

    private UUID id;
    private UUID conversationId;

    private UUID senderUserId;
    private String senderName;

    private UUID senderCompanyId;
    private String senderCompanyName;

    private String body;
    private boolean deleted;
    private LocalDateTime deletedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
