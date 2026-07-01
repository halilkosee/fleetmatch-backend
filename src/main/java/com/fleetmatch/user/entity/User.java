package com.fleetmatch.user.entity;

import com.fleetmatch.common.entity.BaseEntity;
import com.fleetmatch.company.entity.Company;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Version
    private Long version;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    private String phone;

    @Column(nullable = false)
    private boolean emailVerified;

    private LocalDateTime emailVerifiedAt;

    @Column(nullable = false)
    private boolean phoneVerified;

    private LocalDateTime phoneVerifiedAt;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private int failedLoginAttempts;

    private LocalDateTime lockedUntil;

    private LocalDateTime credentialsChangedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlatformRole platformRole = PlatformRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.PENDING_VERIFICATION;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompanyUserRole companyUserRole = CompanyUserRole.OWNER;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;
}
