package com.fleetmatch.notification.repository;

import com.fleetmatch.notification.entity.PushDeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PushDeviceTokenRepository extends JpaRepository<PushDeviceToken, UUID> {

    Optional<PushDeviceToken> findByToken(String token);

    List<PushDeviceToken> findByUserIdAndActiveTrue(UUID userId);

    List<PushDeviceToken> findByUserIdInAndActiveTrue(List<UUID> userIds);
}
