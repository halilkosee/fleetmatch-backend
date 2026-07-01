package com.fleetmatch.user.service;

import com.fleetmatch.audit.service.AuditLogService;
import com.fleetmatch.auth.service.PasswordPolicyService;
import com.fleetmatch.auth.service.VerificationCodeService;
import com.fleetmatch.common.exception.BusinessRuleException;
import com.fleetmatch.security.user.CustomUserDetails;
import com.fleetmatch.user.dto.ChangePasswordRequest;
import com.fleetmatch.user.entity.User;
import com.fleetmatch.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private PasswordPolicyService passwordPolicyService;
    @Mock
    private VerificationCodeService verificationCodeService;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AccountService accountService;

    @Test
    void changePasswordEnforcesNewPolicy() {
        User user = user();
        ChangePasswordRequest request = request("old", "BetterPass!2026");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old", user.getPassword())).thenReturn(true);
        when(passwordEncoder.encode(request.getNewPassword())).thenReturn("new-hash");

        accountService.changePassword(new CustomUserDetails(user), request);

        verify(passwordPolicyService).validate(request.getNewPassword(), user.getEmail());
        assertEquals("new-hash", user.getPassword());
        verify(userRepository).save(user);
    }

    @Test
    void weakPasswordChangeDoesNotModifyExistingHash() {
        User user = user();
        ChangePasswordRequest request = request("old", "weak");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old", user.getPassword())).thenReturn(true);
        doThrow(new BusinessRuleException("Password must be at least 12 characters"))
                .when(passwordPolicyService)
                .validate(request.getNewPassword(), user.getEmail());

        assertThrows(
                BusinessRuleException.class,
                () -> accountService.changePassword(new CustomUserDetails(user), request)
        );

        assertEquals("legacy-hash", user.getPassword());
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

    private ChangePasswordRequest request(String currentPassword, String newPassword) {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword(currentPassword);
        request.setNewPassword(newPassword);
        return request;
    }

    private User user() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setFirstName("Legacy");
        user.setLastName("User");
        user.setEmail("legacy@example.com");
        user.setPassword("legacy-hash");
        return user;
    }
}
