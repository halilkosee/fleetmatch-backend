package com.fleetmatch.support.dto;

import com.fleetmatch.support.entity.SupportMessageSenderType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class SupportMessageResponse {

    private UUID id;
    private UUID ticketId;
    private UUID senderUserId;
    private String senderName;
    private SupportMessageSenderType senderType;
    private String message;
    private LocalDateTime createdAt;
}
