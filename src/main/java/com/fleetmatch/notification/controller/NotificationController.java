package com.fleetmatch.notification.controller;

import com.fleetmatch.notification.dto.NotificationResponse;
import com.fleetmatch.notification.dto.UnreadCountResponse;
import com.fleetmatch.notification.service.NotificationService;
import com.fleetmatch.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "List notifications")
    public Page<NotificationResponse> getNotifications(
            Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return notificationService.getNotifications(currentUser, pageable);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread notification count")
    public UnreadCountResponse getUnreadCount(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return notificationService.getUnreadCount(currentUser);
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "Mark notification as read")
    public NotificationResponse markRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return notificationService.markRead(id, currentUser);
    }

    @PostMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public void markAllRead(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        notificationService.markAllRead(currentUser);
    }
}
