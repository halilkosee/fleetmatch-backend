package com.fleetmatch.messaging.repository;

import com.fleetmatch.messaging.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
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
    Page<Message> findByConversationIdAndDeletedAtIsNullOrderByCreatedAtAsc(
            UUID conversationId,
            Pageable pageable
    );

    List<Message> findByConversationId(UUID conversationId);

    long countByConversationId(UUID conversationId);

    Optional<Message> findTopByConversationIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID conversationId);

    @Query("""
            select count(m)
            from Message m
            where m.conversation.id = :conversationId
              and m.deletedAt is null
              and m.readAt is null
              and m.senderCompany.id <> :companyId
            """)
    long countUnreadForCompany(UUID conversationId, UUID companyId);
}
