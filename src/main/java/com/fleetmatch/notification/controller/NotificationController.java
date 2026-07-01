package com.fleetmatch.notification.controller;

import com.fleetmatch.notification.dto.NotificationResponse;
import com.fleetmatch.notification.dto.PushDeviceResponse;
import com.fleetmatch.notification.dto.RegisterPushDeviceRequest;
import com.fleetmatch.notification.dto.UnreadCountResponse;
import com.fleetmatch.notification.dto.UnregisterPushDeviceRequest;
import com.fleetmatch.notification.service.PushDeviceService;
import com.fleetmatch.notification.service.NotificationService;
import com.fleetmatch.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
    private final PushDeviceService pushDeviceService;

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

    @PostMapping("/devices")
    @Operation(summary = "Register push notification device")
    public PushDeviceResponse registerPushDevice(
            @Valid @RequestBody RegisterPushDeviceRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return pushDeviceService.registerDevice(request, currentUser);
    }

    @PostMapping("/devices/unregister")
    @Operation(summary = "Unregister push notification device")
    public void unregisterPushDevice(
            @Valid @RequestBody UnregisterPushDeviceRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        pushDeviceService.unregisterDevice(request, currentUser);
    }
}
