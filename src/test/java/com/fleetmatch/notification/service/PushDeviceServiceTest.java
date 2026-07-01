package com.fleetmatch.notification.service;

import com.fleetmatch.notification.dto.RegisterPushDeviceRequest;
import com.fleetmatch.notification.dto.UnregisterPushDeviceRequest;
import com.fleetmatch.notification.entity.DevicePlatform;
import com.fleetmatch.notification.entity.PushDeviceToken;
import com.fleetmatch.notification.repository.PushDeviceTokenRepository;
import com.fleetmatch.security.user.CustomUserDetails;
import com.fleetmatch.user.entity.User;
import com.fleetmatch.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PushDeviceServiceTest {

    @Mock
    private PushDeviceTokenRepository pushDeviceTokenRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PushDeviceService pushDeviceService;

    @Test
    void registerDeviceStoresActiveTokenForCurrentUser() {
        User user = user();
        RegisterPushDeviceRequest request = new RegisterPushDeviceRequest();
        request.setToken("fcm-token-123");
        request.setPlatform(DevicePlatform.IOS);
        request.setDeviceId("iphone-1");
        request.setAppVersion("1.0.0");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(pushDeviceTokenRepository.findByToken(request.getToken())).thenReturn(Optional.empty());
        when(pushDeviceTokenRepository.save(any(PushDeviceToken.class))).thenAnswer(invocation -> {
            PushDeviceToken token = invocation.getArgument(0);
            token.setId(UUID.randomUUID());
            return token;
        });

        var response = pushDeviceService.registerDevice(request, new CustomUserDetails(user));

        assertEquals(DevicePlatform.IOS, response.getPlatform());
        assertEquals("iphone-1", response.getDeviceId());
        assertTrue(response.isActive());
        verify(pushDeviceTokenRepository).save(any(PushDeviceToken.class));
    }

    @Test
    void unregisterDeviceDeactivatesOnlyCurrentUsersToken() {
        User user = user();
        PushDeviceToken token = new PushDeviceToken();
        token.setId(UUID.randomUUID());
        token.setUser(user);
        token.setToken("fcm-token-123");
        token.setPlatform(DevicePlatform.ANDROID);
        token.setActive(true);
        UnregisterPushDeviceRequest request = new UnregisterPushDeviceRequest();
        request.setToken(token.getToken());
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(pushDeviceTokenRepository.findByToken(token.getToken())).thenReturn(Optional.of(token));

        pushDeviceService.unregisterDevice(request, new CustomUserDetails(user));

        assertFalse(token.isActive());
        verify(pushDeviceTokenRepository).save(token);
    }

    private User user() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setFirstName("Mobile");
        user.setLastName("User");
        user.setEmail("mobile@example.test");
        user.setPassword("encoded");
        return user;
    }
}
