package com.fleetmatch.notification.dto;

import com.fleetmatch.notification.push.entity.DevicePlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterPushDeviceRequest {

    @NotBlank
    @Size(max = 500)
    private String token;

    @NotNull
    private DevicePlatform platform;

    @Size(max = 255)
    private String deviceId;

    @Size(max = 100)
    private String appVersion;
}
