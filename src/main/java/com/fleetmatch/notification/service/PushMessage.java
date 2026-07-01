package com.fleetmatch.notification.service;

import com.fleetmatch.notification.entity.NotificationType;

import java.util.UUID;

public record PushMessage(
        String token,
        String title,
        String body,
        NotificationType type,
        String relatedEntityType,
        UUID relatedEntityId,
        UUID notificationId
) {
}
