package com.fleetmatch.notification.service;

import com.fleetmatch.common.exception.ResourceNotFoundException;
import com.fleetmatch.notification.dto.PushDeviceResponse;
import com.fleetmatch.notification.dto.RegisterPushDeviceRequest;
import com.fleetmatch.notification.dto.UnregisterPushDeviceRequest;
import com.fleetmatch.notification.entity.PushDeviceToken;
import com.fleetmatch.notification.repository.PushDeviceTokenRepository;
import com.fleetmatch.security.user.CustomUserDetails;
import com.fleetmatch.user.entity.User;
import com.fleetmatch.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PushDeviceService {

    private final PushDeviceTokenRepository pushDeviceTokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public PushDeviceResponse registerDevice(
            RegisterPushDeviceRequest request,
            CustomUserDetails currentUser
    ) {
        User user = getCurrentUser(currentUser);
        LocalDateTime now = LocalDateTime.now();

        PushDeviceToken token = pushDeviceTokenRepository.findByToken(request.getToken())
                .orElseGet(PushDeviceToken::new);

        token.setUser(user);
        token.setToken(request.getToken());
        token.setPlatform(request.getPlatform());
        token.setDeviceId(request.getDeviceId());
        token.setAppVersion(request.getAppVersion());
        token.setActive(true);
        token.setLastRegisteredAt(now);
        token.setLastUsedAt(now);

        return toResponse(pushDeviceTokenRepository.save(token));
    }

    @Transactional
    public void unregisterDevice(
            UnregisterPushDeviceRequest request,
            CustomUserDetails currentUser
    ) {
        User user = getCurrentUser(currentUser);

        pushDeviceTokenRepository.findByToken(request.getToken())
                .filter(token -> token.getUser().getId().equals(user.getId()))
                .ifPresent(token -> {
                    token.setActive(false);
                    token.setLastUsedAt(LocalDateTime.now());
                    pushDeviceTokenRepository.save(token);
                });
    }

    private User getCurrentUser(CustomUserDetails currentUser) {
        return userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private PushDeviceResponse toResponse(PushDeviceToken token) {
        return PushDeviceResponse.builder()
                .id(token.getId())
                .platform(token.getPlatform())
                .deviceId(token.getDeviceId())
                .appVersion(token.getAppVersion())
                .active(token.isActive())
                .lastRegisteredAt(token.getLastRegisteredAt())
                .build();
    }
}
