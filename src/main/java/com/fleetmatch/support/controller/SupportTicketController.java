package com.fleetmatch.support.controller;

import com.fleetmatch.security.user.CustomUserDetails;
import com.fleetmatch.support.dto.CloseSupportTicketRequest;
import com.fleetmatch.support.dto.CreateSupportTicketRequest;
import com.fleetmatch.support.dto.SendSupportMessageRequest;
import com.fleetmatch.support.dto.SupportMessageResponse;
import com.fleetmatch.support.dto.SupportTicketResponse;
import com.fleetmatch.support.service.SupportTicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/support/tickets")
@RequiredArgsConstructor
public class SupportTicketController {

    private final SupportTicketService supportTicketService;

    @PostMapping
    public SupportTicketResponse createTicket(
            @Valid @RequestBody CreateSupportTicketRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return supportTicketService.createTicket(request, currentUser);
    }

    @GetMapping
    public List<SupportTicketResponse> myTickets(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return supportTicketService.getMyTickets(currentUser);
    }

    @GetMapping("/{ticketId}/messages")
    public List<SupportMessageResponse> messages(
            @PathVariable UUID ticketId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return supportTicketService.getUserMessages(ticketId, currentUser);
    }

    @PostMapping("/{ticketId}/messages")
    public SupportMessageResponse sendMessage(
            @PathVariable UUID ticketId,
            @Valid @RequestBody SendSupportMessageRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return supportTicketService.sendUserMessage(
                ticketId,
                request,
                currentUser
        );
    }

    @PatchMapping("/{ticketId}/close")
    public SupportTicketResponse closeTicket(
            @PathVariable UUID ticketId,
            @Valid @RequestBody CloseSupportTicketRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return supportTicketService.closeUserTicket(
                ticketId,
                request,
                currentUser
        );
    }
}
