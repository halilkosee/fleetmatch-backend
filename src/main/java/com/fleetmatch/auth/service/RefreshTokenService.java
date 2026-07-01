package com.fleetmatch.auth.service;

import com.fleetmatch.auth.entity.RefreshToken;
import com.fleetmatch.auth.repository.RefreshTokenRepository;
import com.fleetmatch.common.exception.BusinessRuleException;
import com.fleetmatch.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${fleetmatch.jwt.refresh-expiration-ms:2592000000}")
    private long refreshExpirationMs;

    @Transactional
    public String issue(User user, String userAgent, String ipAddress) {
        String rawToken = generateRawToken();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hash(rawToken));
        refreshToken.setExpiresAt(expiresAt());
        refreshToken.setUserAgent(userAgent);
        refreshToken.setIpAddress(ipAddress);
        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    @Transactional
    public RefreshTokenIssue rotate(String rawToken, String userAgent, String ipAddress) {
        RefreshToken existing = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new BusinessRuleException("Invalid refresh token"));

        if (!existing.isActive()) {
            throw new BusinessRuleException("Refresh token expired or revoked");
        }

        String replacement = generateRawToken();
        String replacementHash = hash(replacement);

        existing.setRevokedAt(LocalDateTime.now());
        existing.setReplacedByTokenHash(replacementHash);
        refreshTokenRepository.save(existing);

        RefreshToken next = new RefreshToken();
        next.setUser(existing.getUser());
        next.setTokenHash(replacementHash);
        next.setExpiresAt(expiresAt());
        next.setUserAgent(userAgent);
        next.setIpAddress(ipAddress);
        refreshTokenRepository.save(next);

        return new RefreshTokenIssue(existing.getUser(), replacement);
    }

    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(hash(rawToken))
                .filter(RefreshToken::isActive)
                .ifPresent(refreshToken -> {
                    refreshToken.setRevokedAt(LocalDateTime.now());
                    refreshTokenRepository.save(refreshToken);
                });
    }

    @Transactional
    public void revokeAll(User user) {
        refreshTokenRepository
                .findByUserIdAndRevokedAtIsNullAndExpiresAtAfter(user.getId(), LocalDateTime.now())
                .forEach(refreshToken -> {
                    refreshToken.setRevokedAt(LocalDateTime.now());
                    refreshTokenRepository.save(refreshToken);
                });
    }

    private String generateRawToken() {
        byte[] bytes = new byte[64];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private LocalDateTime expiresAt() {
        return LocalDateTime.now().plus(Duration.ofMillis(refreshExpirationMs));
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    public record RefreshTokenIssue(User user, String refreshToken) {
    }
}
