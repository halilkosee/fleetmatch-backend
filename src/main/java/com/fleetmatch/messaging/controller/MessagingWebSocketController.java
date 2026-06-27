package com.fleetmatch.messaging.controller;

import com.fleetmatch.messaging.dto.CreateMessageRequest;
import com.fleetmatch.messaging.dto.MessageResponse;
import com.fleetmatch.messaging.service.MessagingService;
import com.fleetmatch.security.user.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

import java.security.Principal;
import java.util.UUID;

@Controller
@Validated
@RequiredArgsConstructor
public class MessagingWebSocketController {

    private final MessagingService messagingService;

    @MessageMapping("/conversations/{conversationId}/messages")
    public MessageResponse sendMessage(
            @DestinationVariable UUID conversationId,
            @Valid CreateMessageRequest request,
            Principal principal
    ) {
        return messagingService.sendMessage(
                conversationId,
                request,
                currentUser(principal)
        );
    }

    private CustomUserDetails currentUser(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken authentication &&
                authentication.getPrincipal() instanceof CustomUserDetails currentUser) {
            return currentUser;
        }

        throw new IllegalStateException("Authenticated websocket user is required");
    }
}
