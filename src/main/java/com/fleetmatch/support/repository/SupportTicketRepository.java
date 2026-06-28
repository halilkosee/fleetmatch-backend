package com.fleetmatch.support.repository;

import com.fleetmatch.support.entity.SupportTicket;
import com.fleetmatch.support.entity.SupportTicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, UUID> {

    List<SupportTicket> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<SupportTicket> findByStatusOrderByCreatedAtAsc(SupportTicketStatus status);
}
