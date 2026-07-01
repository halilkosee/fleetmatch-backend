package com.fleetmatch.support.dto;

import com.fleetmatch.company.entity.CompanyType;
import com.fleetmatch.support.category.SupportTicketCategory;
import com.fleetmatch.support.category.SupportTicketPriority;
import com.fleetmatch.support.ticket.entity.SupportTicketStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class SupportTicketResponse {

    private UUID id;
    private UUID userId;
    private String userName;
    private String userEmail;
    private UUID companyId;
    private String companyName;
    private CompanyType companyType;
    private SupportTicketCategory category;
    private SupportTicketPriority priority;
    private String subject;
    private String message;
    private SupportTicketStatus status;
    private String adminReply;
    private LocalDateTime createdAt;
    private LocalDateTime expectedResponseAt;
    private LocalDateTime answeredAt;
    private LocalDateTime closedAt;
    private String resolutionSummary;
    private Integer satisfactionRating;
    private String satisfactionComment;
}
