package com.fleetmatch.messaging.controller;

import com.fleetmatch.messaging.dto.ConversationResponse;
import com.fleetmatch.messaging.dto.CreateMessageRequest;
import com.fleetmatch.messaging.dto.MessageResponse;
import com.fleetmatch.messaging.service.MessagingService;
import com.fleetmatch.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Messaging")
@SecurityRequirement(name = "bearerAuth")
public class MessagingController {

    private final MessagingService messagingService;

    @GetMapping("/conversations")
    @Operation(summary = "List conversations for the authenticated company")
    public Page<ConversationResponse> getConversations(
            Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return messagingService.getConversations(
                pageable,
                currentUser
        );
    }

    @GetMapping("/loads/{loadId}/conversation")
    @Operation(summary = "Get the conversation created for an accepted load")
    public ConversationResponse getConversationByLoad(
            @PathVariable UUID loadId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return messagingService.getConversationByLoad(
                loadId,
                currentUser
        );
    }

    @GetMapping("/conversations/{conversationId}/messages")
    @Operation(summary = "Page messages in a conversation")
    public Page<MessageResponse> getMessages(
            @PathVariable UUID conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return messagingService.getMessages(
                conversationId,
                PageRequest.of(page, size),
                currentUser
        );
    }

    @PostMapping("/conversations/{conversationId}/messages")
    @Operation(summary = "Send a message in a conversation")
    public MessageResponse sendMessage(
            @PathVariable UUID conversationId,
            @Valid @RequestBody CreateMessageRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return messagingService.sendMessage(
                conversationId,
                request,
                currentUser
        );
    }

    @DeleteMapping("/conversations/{conversationId}/messages/{messageId}")
    @Operation(summary = "Soft delete a message")
    public MessageResponse deleteMessage(
            @PathVariable UUID conversationId,
            @PathVariable UUID messageId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return messagingService.deleteMessage(
                conversationId,
                messageId,
                currentUser
        );
    }

    @PutMapping("/conversations/{conversationId}/messages/{messageId}/read")
    @Operation(summary = "Mark a message as read")
    public MessageResponse markMessageAsRead(
            @PathVariable UUID conversationId,
            @PathVariable UUID messageId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return messagingService.markMessageAsRead(
                conversationId,
                messageId,
                currentUser
        );
    }
}
