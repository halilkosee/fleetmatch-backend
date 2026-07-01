package com.fleetmatch.support.ticket.entity;

import com.fleetmatch.common.entity.BaseEntity;
import com.fleetmatch.company.entity.Company;
import com.fleetmatch.support.category.SupportTicketCategory;
import com.fleetmatch.support.category.SupportTicketPriority;
import com.fleetmatch.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "support_tickets")
public class SupportTicket extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SupportTicketCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SupportTicketPriority priority;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false, length = 4000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SupportTicketStatus status = SupportTicketStatus.WAITING_ADMIN;

    @Column(length = 4000)
    private String adminReply;

    private LocalDateTime expectedResponseAt;

    private LocalDateTime answeredAt;

    private LocalDateTime closedAt;

    @Column(length = 1000)
    private String resolutionSummary;

    private Integer satisfactionRating;

    @Column(length = 1000)
    private String satisfactionComment;
}
