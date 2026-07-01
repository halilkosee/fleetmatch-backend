package com.fleetmatch.notification.dto;

import com.fleetmatch.notification.push.entity.DevicePlatform;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class PushDeviceResponse {

    private UUID id;
    private DevicePlatform platform;
    private String deviceId;
    private String appVersion;
    private boolean active;
    private LocalDateTime lastRegisteredAt;
}
