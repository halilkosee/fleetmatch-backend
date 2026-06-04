package com.fleetmatch.admin.dto;

import com.fleetmatch.user.entity.PlatformRole;
import com.fleetmatch.user.entity.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class PendingUserResponse {

    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private PlatformRole platformRole;
    private UserStatus status;
    private String companyName;
}