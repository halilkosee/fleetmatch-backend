package com.fleetmatch.support.ticket.controller;

import com.fleetmatch.security.user.CustomUserDetails;
import com.fleetmatch.support.dto.CloseSupportTicketRequest;
import com.fleetmatch.support.dto.SendSupportMessageRequest;
import com.fleetmatch.support.dto.SupportMessageResponse;
import com.fleetmatch.support.dto.SupportReplyTemplateRequest;
import com.fleetmatch.support.dto.SupportReplyTemplateResponse;
import com.fleetmatch.support.dto.SupportTicketResponse;
import com.fleetmatch.support.ticket.entity.SupportTicketStatus;
import com.fleetmatch.support.ticket.service.SupportTicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/support")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSupportController {

    private final SupportTicketService supportTicketService;

    @GetMapping("/tickets")
    public List<SupportTicketResponse> tickets(
            @RequestParam(required = false) SupportTicketStatus status
    ) {
        return supportTicketService.getAdminTickets(status);
    }

    @GetMapping("/tickets/{ticketId}")
    public SupportTicketResponse ticket(
            @PathVariable UUID ticketId
    ) {
        return supportTicketService.getAdminTicket(ticketId);
    }

    @GetMapping("/tickets/{ticketId}/messages")
    public List<SupportMessageResponse> messages(
            @PathVariable UUID ticketId
    ) {
        return supportTicketService.getAdminMessages(ticketId);
    }

    @PostMapping("/tickets/{ticketId}/messages")
    public SupportMessageResponse sendMessage(
            @PathVariable UUID ticketId,
            @Valid @RequestBody SendSupportMessageRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return supportTicketService.sendAdminMessage(
                ticketId,
                request,
                currentUser
        );
    }

    @PatchMapping("/tickets/{ticketId}/close")
    public SupportTicketResponse closeTicket(
            @PathVariable UUID ticketId,
            @RequestBody(required = false) CloseSupportTicketRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return supportTicketService.closeTicket(ticketId, request, currentUser);
    }

    @GetMapping("/templates")
    public List<SupportReplyTemplateResponse> templates() {
        return supportTicketService.getTemplates();
    }

    @PostMapping("/templates")
    public SupportReplyTemplateResponse createTemplate(
            @Valid @RequestBody SupportReplyTemplateRequest request
    ) {
        return supportTicketService.createTemplate(request);
    }

    @PutMapping("/templates/{templateId}")
    public SupportReplyTemplateResponse updateTemplate(
            @PathVariable UUID templateId,
            @Valid @RequestBody SupportReplyTemplateRequest request
    ) {
        return supportTicketService.updateTemplate(templateId, request);
    }
}
