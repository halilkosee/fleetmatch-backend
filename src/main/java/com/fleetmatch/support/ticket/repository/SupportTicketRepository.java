package com.fleetmatch.support.ticket.repository;

import com.fleetmatch.support.ticket.entity.SupportTicket;
import com.fleetmatch.support.ticket.entity.SupportTicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, UUID> {

    List<SupportTicket> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<SupportTicket> findByStatusOrderByCreatedAtAsc(SupportTicketStatus status);
}
