package com.fleetmatch.user.dto;

import com.fleetmatch.company.entity.CompanyType;
import com.fleetmatch.user.entity.CompanyUserRole;
import com.fleetmatch.user.entity.PlatformRole;
import com.fleetmatch.user.entity.UserStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class UserAccountResponse {

    private UUID id;

    private String firstName;

    private String lastName;

    private String email;

    private boolean emailVerified;

    private LocalDateTime emailVerifiedAt;

    private String phone;

    private boolean phoneVerified;

    private LocalDateTime phoneVerifiedAt;

    private PlatformRole platformRole;

    private CompanyUserRole companyUserRole;

    private UserStatus status;

    private UUID companyId;

    private String companyName;

    private CompanyType companyType;
}
