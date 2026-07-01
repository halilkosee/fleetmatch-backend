package com.fleetmatch.notification.entity;

import com.fleetmatch.common.entity.BaseEntity;
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
@Table(name = "notification_deliveries")
public class NotificationDelivery extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id")
    private Notification notification;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_token_id")
    private PushDeviceToken deviceToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationDeliveryChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationDeliveryStatus status;

    @Column(length = 100)
    private String provider;

    private String providerMessageId;

    @Column(length = 1000)
    private String errorMessage;

    private LocalDateTime sentAt;
}
