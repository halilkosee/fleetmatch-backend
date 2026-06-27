package com.fleetmatch.notification.dto;

import com.fleetmatch.notification.entity.NotificationType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class NotificationResponse {

    private UUID id;
    private NotificationType type;
    private String title;
    private String message;
    private LocalDateTime readAt;
    private String relatedEntityType;
    private UUID relatedEntityId;
    private LocalDateTime createdAt;
}
