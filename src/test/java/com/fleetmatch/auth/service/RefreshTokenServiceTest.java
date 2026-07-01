package com.fleetmatch.auth.service;

import com.fleetmatch.auth.entity.RefreshToken;
import com.fleetmatch.auth.repository.RefreshTokenRepository;
import com.fleetmatch.common.exception.BusinessRuleException;
import com.fleetmatch.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Test
    void issueStoresOnlyTokenHash() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshExpirationMs", 1000L);
        User user = user();

        String rawToken = refreshTokenService.issue(user, "Mobile", "127.0.0.1");

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken saved = captor.getValue();
        assertNotNull(rawToken);
        assertNotEquals(rawToken, saved.getTokenHash());
        assertEquals(user, saved.getUser());
        assertEquals("Mobile", saved.getUserAgent());
        assertEquals("127.0.0.1", saved.getIpAddress());
        assertTrue(saved.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    @Test
    void rotateRejectsUnknownToken() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThrows(
                BusinessRuleException.class,
                () -> refreshTokenService.rotate("missing", "Mobile", "127.0.0.1")
        );
    }

    @Test
    void revokeMarksActiveTokenRevoked() {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(1));
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(refreshToken));

        refreshTokenService.revoke("refresh-token");

        assertNotNull(refreshToken.getRevokedAt());
        verify(refreshTokenRepository).save(refreshToken);
    }

    private User user() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("ada@atlas.test");
        return user;
    }
}
