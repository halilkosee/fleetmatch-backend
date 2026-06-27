package com.fleetmatch.auth.repository;

import com.fleetmatch.auth.entity.UserVerificationCode;
import com.fleetmatch.auth.entity.VerificationChannel;
import com.fleetmatch.auth.entity.VerificationPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface UserVerificationCodeRepository extends JpaRepository<UserVerificationCode, UUID> {

    Optional<UserVerificationCode> findTopByUserIdAndPurposeAndChannelAndTargetValueAndUsedAtIsNullOrderByCreatedAtDesc(
            UUID userId,
            VerificationPurpose purpose,
            VerificationChannel channel,
            String targetValue
    );

    Optional<UserVerificationCode> findTopByPurposeAndChannelAndTargetValueAndUsedAtIsNullOrderByCreatedAtDesc(
            VerificationPurpose purpose,
            VerificationChannel channel,
            String targetValue
    );

    @Modifying
    @Query("""
            update UserVerificationCode code
            set code.usedAt = CURRENT_TIMESTAMP
            where code.user.id = :userId
              and code.purpose = :purpose
              and code.channel = :channel
              and code.targetValue = :targetValue
              and code.usedAt is null
            """)
    void invalidateActiveCodes(
            UUID userId,
            VerificationPurpose purpose,
            VerificationChannel channel,
            String targetValue
    );
}
