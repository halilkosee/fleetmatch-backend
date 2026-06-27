package com.fleetmatch.messaging.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class ConversationStreamEvent {

    private String type;
    private UUID conversationId;
    private LocalDateTime createdAt;
    private MessageResponse message;
}
