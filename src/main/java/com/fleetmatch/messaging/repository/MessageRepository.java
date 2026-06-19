package com.fleetmatch.messaging.repository;

import com.fleetmatch.messaging.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    @Override
    @EntityGraph(attributePaths = {
            "conversation",
            "senderUser",
            "senderCompany"
    })
    Optional<Message> findById(UUID id);

    @EntityGraph(attributePaths = {
            "conversation",
            "senderUser",
            "senderCompany"
    })
    Page<Message> findByConversationIdOrderByCreatedAtAsc(
            UUID conversationId,
            Pageable pageable
    );
}
