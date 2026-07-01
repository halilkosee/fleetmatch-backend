package com.fleetmatch.notification.inapp.repository;

import com.fleetmatch.notification.inapp.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByUserIdOrCompanyIdOrderByCreatedAtDesc(
            UUID userId,
            UUID companyId,
            Pageable pageable
    );

    @Query("""
            select count(n)
            from Notification n
            where n.readAt is null
              and (
                n.user.id = :userId
                or n.company.id = :companyId
              )
            """)
    long countUnreadForUserOrCompany(
            UUID userId,
            UUID companyId
    );
}
