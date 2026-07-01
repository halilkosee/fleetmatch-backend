package com.fleetmatch.auth.service;

import com.fleetmatch.auth.dto.AuthResponse;
import com.fleetmatch.auth.dto.RegisterRequest;
import com.fleetmatch.auth.dto.ResetPasswordRequest;
import com.fleetmatch.auth.dto.VerifyEmailRequest;
import com.fleetmatch.auth.dto.VerifyPhoneRequest;
import com.fleetmatch.auth.entity.VerificationChannel;
import com.fleetmatch.auth.entity.VerificationPurpose;
import com.fleetmatch.audit.service.AuditLogService;
import com.fleetmatch.common.exception.BusinessRuleException;
import com.fleetmatch.company.entity.Company;
import com.fleetmatch.company.entity.CompanyType;
import com.fleetmatch.company.repository.CompanyRepository;
import com.fleetmatch.security.jwt.JwtService;
import com.fleetmatch.security.user.CustomUserDetails;
import com.fleetmatch.security.user.CustomUserDetailsService;
import com.fleetmatch.user.entity.User;
import com.fleetmatch.user.entity.UserStatus;
import com.fleetmatch.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private CustomUserDetailsService userDetailsService;
    @Mock
    private PasswordPolicyService passwordPolicyService;
    @Mock
    private VerificationCodeService verificationCodeService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerCreatesCompanyOwnerAndVerificationCodes() {
        RegisterRequest request = registerRequest();
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.existsByPhone(request.getPhone())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded-password");
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> {
            Company company = invocation.getArgument(0);
            company.setId(UUID.randomUUID());
            return company;
        });
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        authService.register(request);

        verify(passwordPolicyService).validate(request.getPassword(), request.getEmail());
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertEquals(UserStatus.REGISTERED, saved.getStatus());
        assertEquals("encoded-password", saved.getPassword());
        assertEquals(CompanyType.BROKER, saved.getCompany().getType());
        verify(verificationCodeService).createCode(
                saved,
                VerificationPurpose.EMAIL_VERIFICATION,
                VerificationChannel.EMAIL,
                saved.getEmail()
        );
        verify(verificationCodeService).createCode(
                saved,
                VerificationPurpose.PHONE_VERIFICATION,
                VerificationChannel.PHONE,
                saved.getPhone()
        );
    }

    @Test
    void weakRegistrationPasswordIsRejected() {
        RegisterRequest request = registerRequest();
        request.setPassword("weak");
        doThrow(new BusinessRuleException("Password must be at least 12 characters"))
                .when(passwordPolicyService)
                .validate(request.getPassword(), request.getEmail());
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.existsByPhone(request.getPhone())).thenReturn(false);

        assertThrows(
                BusinessRuleException.class,
                () -> authService.register(request)
        );

        verify(companyRepository, never()).save(any());
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void loginReturnsTokenAndResetsFailedAttempts() {
        User user = user();
        user.setFailedLoginAttempts(2);
        when(userDetailsService.loadUserByUsername(user.getEmail()))
                .thenReturn(new CustomUserDetails(user));
        when(passwordEncoder.matches("CorrectPass!123", user.getPassword()))
                .thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");
        when(jwtService.getExpirationMillis()).thenReturn(900000L);
        when(refreshTokenService.issue(user, null, null)).thenReturn("refresh-token");

        AuthResponse response = authService.login(user.getEmail(), "CorrectPass!123");

        assertEquals("jwt-token", response.getToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(900000L, response.getExpiresInMs());
        assertEquals(0, user.getFailedLoginAttempts());
        verify(userRepository).save(user);
        verify(passwordPolicyService, never()).validate(any());
        verify(passwordPolicyService, never()).validate(any(), any());
    }

    @Test
    void existingUserCanStillLoginWithLegacyPassword() {
        User user = user();
        user.setPassword("legacy-hash");
        when(userDetailsService.loadUserByUsername(user.getEmail()))
                .thenReturn(new CustomUserDetails(user));
        when(passwordEncoder.matches("old", user.getPassword()))
                .thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");
        when(jwtService.getExpirationMillis()).thenReturn(900000L);
        when(refreshTokenService.issue(user, null, null)).thenReturn("refresh-token");

        AuthResponse response = authService.login(user.getEmail(), "old");

        assertEquals("jwt-token", response.getToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("legacy-hash", user.getPassword());
        verify(passwordPolicyService, never()).validate(any());
        verify(passwordPolicyService, never()).validate(any(), any());
    }

    @Test
    void loginRecordsFailedAttemptForBadPassword() {
        User user = user();
        when(userDetailsService.loadUserByUsername(user.getEmail()))
                .thenReturn(new CustomUserDetails(user));
        when(passwordEncoder.matches("bad", user.getPassword())).thenReturn(false);

        assertThrows(
                BadCredentialsException.class,
                () -> authService.login(user.getEmail(), "bad")
        );

        assertEquals(1, user.getFailedLoginAttempts());
        verify(userRepository).save(user);
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void refreshRotatesRefreshTokenAndReturnsNewAccessToken() {
        User user = user();
        when(refreshTokenService.rotate("old-refresh-token", "Mobile", "127.0.0.1"))
                .thenReturn(new RefreshTokenService.RefreshTokenIssue(user, "new-refresh-token"));
        when(jwtService.generateToken(user)).thenReturn("new-jwt-token");
        when(jwtService.getExpirationMillis()).thenReturn(900000L);

        AuthResponse response = authService.refresh(
                "old-refresh-token",
                "Mobile",
                "127.0.0.1"
        );

        assertEquals("new-jwt-token", response.getToken());
        assertEquals("new-refresh-token", response.getRefreshToken());
        assertEquals(900000L, response.getExpiresInMs());
    }

    @Test
    void logoutRevokesRefreshToken() {
        authService.logout("refresh-token");

        verify(refreshTokenService).revoke("refresh-token");
    }

    @Test
    void verifyEmailAdvancesRegisteredUser() {
        User user = user();
        user.setStatus(UserStatus.REGISTERED);
        user.setEmailVerified(false);
        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setEmail(user.getEmail());
        request.setCode("123456");
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        authService.verifyEmail(request);

        assertTrue(user.isEmailVerified());
        assertEquals(UserStatus.EMAIL_VERIFIED, user.getStatus());
        verify(verificationCodeService).verifyCode(
                user.getId(),
                VerificationPurpose.EMAIL_VERIFICATION,
                VerificationChannel.EMAIL,
                user.getEmail(),
                "123456"
        );
        verify(userRepository).save(user);
    }

    @Test
    void verifyPhoneAdvancesOnlyAfterEmailIsVerified() {
        User user = user();
        user.setStatus(UserStatus.EMAIL_VERIFIED);
        user.setEmailVerified(true);
        user.setPhoneVerified(false);
        VerifyPhoneRequest request = new VerifyPhoneRequest();
        request.setPhone(user.getPhone());
        request.setCode("654321");
        when(userRepository.findByPhone(user.getPhone())).thenReturn(Optional.of(user));

        authService.verifyPhone(request);

        assertTrue(user.isPhoneVerified());
        assertEquals(UserStatus.PHONE_VERIFIED, user.getStatus());
        verify(verificationCodeService).verifyCode(
                user.getId(),
                VerificationPurpose.PHONE_VERIFICATION,
                VerificationChannel.PHONE,
                user.getPhone(),
                "654321"
        );
        verify(userRepository).save(user);
    }

    @Test
    void resetPasswordEnforcesPolicyBeforeHashing() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("ada@atlas.test");
        request.setCode("123456");
        request.setNewPassword("BetterPass!123");
        User user = user();
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(request.getNewPassword())).thenReturn("new-hash");

        authService.resetPassword(request);

        verify(passwordPolicyService).validate(request.getNewPassword(), request.getEmail());
        assertEquals("new-hash", user.getPassword());
        verify(userRepository).save(user);
    }

    @Test
    void weakPasswordResetDoesNotModifyExistingHash() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("ada@atlas.test");
        request.setCode("123456");
        request.setNewPassword("weak");
        doThrow(new BusinessRuleException("Password must be at least 12 characters"))
                .when(passwordPolicyService)
                .validate(request.getNewPassword(), request.getEmail());

        assertThrows(
                BusinessRuleException.class,
                () -> authService.resetPassword(request)
        );

        verify(userRepository, never()).findByEmail(any());
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

    private RegisterRequest registerRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setCompanyLegalName("Atlas Freight LLC");
        request.setCompanyEmail("ops@atlas.test");
        request.setCompanyPhone("+15550100");
        request.setCompanyType(CompanyType.BROKER);
        request.setFirstName("Ada");
        request.setLastName("Owner");
        request.setEmail("ada@atlas.test");
        request.setPhone("+15550101");
        request.setPassword("CorrectPass!123");
        return request;
    }

    private User user() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setFirstName("Ada");
        user.setLastName("Owner");
        user.setEmail("ada@atlas.test");
        user.setPhone("+15550101");
        user.setPassword("encoded-password");
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.setPhoneVerified(true);
        return user;
    }
}
