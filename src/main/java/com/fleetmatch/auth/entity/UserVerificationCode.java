package com.fleetmatch.auth.entity;

import com.fleetmatch.common.entity.BaseEntity;
import com.fleetmatch.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(
        name = "user_verification_codes",
        indexes = {
                @Index(name = "idx_user_verification_codes_user_purpose", columnList = "user_id,purpose"),
                @Index(name = "idx_user_verification_codes_expires_at", columnList = "expires_at")
        }
)
public class UserVerificationCode extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private VerificationPurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VerificationChannel channel;

    @Column(name = "target_value", nullable = false, length = 255)
    private String targetValue;

    @Column(name = "code_hash", nullable = false, length = 255)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    public boolean isUsed() {
        return usedAt != null;
    }
}
