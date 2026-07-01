package com.fleetmatch.notification.push.repository;

import com.fleetmatch.notification.push.entity.NotificationDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, UUID> {
}
