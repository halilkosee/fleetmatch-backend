package com.fleetmatch.auth.service;

import com.fleetmatch.auth.dto.VerificationCodeResponse;
import com.fleetmatch.auth.entity.UserVerificationCode;
import com.fleetmatch.auth.entity.VerificationChannel;
import com.fleetmatch.auth.entity.VerificationPurpose;
import com.fleetmatch.auth.repository.UserVerificationCodeRepository;
import com.fleetmatch.common.exception.BusinessRuleException;
import com.fleetmatch.common.exception.ResourceNotFoundException;
import com.fleetmatch.user.entity.User;
import com.fleetmatch.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VerificationCodeService {

    private static final int CODE_TTL_MINUTES = 10;
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserVerificationCodeRepository codeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SmsService smsService;

    @Value("${fleetmatch.environment:LOCAL}")
    private String environment;

    @Transactional
    public VerificationCodeResponse createCode(
            User user,
            VerificationPurpose purpose,
            VerificationChannel channel,
            String targetValue
    ) {
        codeRepository.invalidateActiveCodes(
                user.getId(),
                purpose,
                channel,
                targetValue
        );

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));

        UserVerificationCode verificationCode = new UserVerificationCode();
        verificationCode.setUser(user);
        verificationCode.setPurpose(purpose);
        verificationCode.setChannel(channel);
        verificationCode.setTargetValue(targetValue);
        verificationCode.setCodeHash(passwordEncoder.encode(code));
        verificationCode.setExpiresAt(LocalDateTime.now().plusMinutes(CODE_TTL_MINUTES));

        codeRepository.save(verificationCode);

        if (channel == VerificationChannel.EMAIL) {
            emailService.sendOtp(targetValue, code, purpose.name());
        } else {
            smsService.sendOtp(targetValue, code, purpose.name());
        }

        return new VerificationCodeResponse(
                "Verification code sent",
                shouldExposeDebugCode() ? code : null
        );
    }

    @Transactional
    public User verifyCode(
            UUID userId,
            VerificationPurpose purpose,
            VerificationChannel channel,
            String targetValue,
            String code
    ) {
        UserVerificationCode verificationCode =
                codeRepository
                        .findTopByUserIdAndPurposeAndChannelAndTargetValueAndUsedAtIsNullOrderByCreatedAtDesc(
                                userId,
                                purpose,
                                channel,
                                targetValue
                        )
                        .orElseThrow(() -> new BusinessRuleException(
                                "Invalid or expired verification code"
                        ));

        validateCode(verificationCode, code);
        verificationCode.setUsedAt(LocalDateTime.now());

        return verificationCode.getUser();
    }

    @Transactional
    public User verifyCodeForTarget(
            VerificationPurpose purpose,
            VerificationChannel channel,
            String targetValue,
            String code
    ) {
        UserVerificationCode verificationCode =
                codeRepository
                        .findTopByPurposeAndChannelAndTargetValueAndUsedAtIsNullOrderByCreatedAtDesc(
                                purpose,
                                channel,
                                targetValue
                        )
                        .orElseThrow(() -> new BusinessRuleException(
                                "Invalid or expired verification code"
                        ));

        validateCode(verificationCode, code);
        verificationCode.setUsedAt(LocalDateTime.now());

        return verificationCode.getUser();
    }

    public User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private void validateCode(UserVerificationCode verificationCode, String code) {
        if (verificationCode.isUsed() ||
                verificationCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("Invalid or expired verification code");
        }

        if (verificationCode.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
            throw new BusinessRuleException("Too many failed verification attempts");
        }

        if (!passwordEncoder.matches(code, verificationCode.getCodeHash())) {
            verificationCode.setFailedAttempts(
                    verificationCode.getFailedAttempts() + 1
            );
            throw new BusinessRuleException("Invalid or expired verification code");
        }
    }

    private boolean shouldExposeDebugCode() {
        return !"PROD".equalsIgnoreCase(environment) &&
                !"prod".equalsIgnoreCase(environment);
    }
}
