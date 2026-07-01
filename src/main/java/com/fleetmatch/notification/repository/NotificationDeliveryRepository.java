package com.fleetmatch.notification.repository;

import com.fleetmatch.notification.entity.NotificationDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, UUID> {
}
