package com.fleetmatch.messaging.controller;

import com.fleetmatch.messaging.service.ConversationRealtimeService;
import com.fleetmatch.messaging.service.MessagingService;
import com.fleetmatch.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/ws")
@RequiredArgsConstructor
@Tag(name = "Messaging Realtime")
@SecurityRequirement(name = "bearerAuth")
public class MessagingRealtimeController {

    private final MessagingService messagingService;
    private final ConversationRealtimeService conversationRealtimeService;

    @GetMapping(value = "/conversations/{conversationId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Subscribe to realtime conversation events")
    public SseEmitter subscribe(
            @PathVariable UUID conversationId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        messagingService.validateConversationAccess(conversationId, currentUser);
        return conversationRealtimeService.subscribe(conversationId);
    }
}
