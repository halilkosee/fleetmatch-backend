package com.fleetmatch.messaging.repository;

import com.fleetmatch.messaging.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    Optional<Conversation> findByLoadId(UUID loadId);

    @Query("""
            select c
            from Conversation c
            where c.brokerCompany.id = :companyId
               or c.fleetCompany.id = :companyId
            """)
    Page<Conversation> findByParticipantCompanyId(
            @Param("companyId") UUID companyId,
            Pageable pageable
    );
}
