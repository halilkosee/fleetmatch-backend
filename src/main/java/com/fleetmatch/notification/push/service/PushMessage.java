package com.fleetmatch.notification.push.service;

import com.fleetmatch.notification.event.NotificationType;

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
