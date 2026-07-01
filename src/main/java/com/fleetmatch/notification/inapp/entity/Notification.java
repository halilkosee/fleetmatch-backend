package com.fleetmatch.notification.inapp.entity;

import com.fleetmatch.common.entity.BaseEntity;
import com.fleetmatch.company.entity.Company;
import com.fleetmatch.notification.event.NotificationType;
import com.fleetmatch.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "notifications")
public class Notification extends BaseEntity {

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 80)
    private NotificationType type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    private LocalDateTime readAt;

    private String relatedEntityType;

    private UUID relatedEntityId;
}
