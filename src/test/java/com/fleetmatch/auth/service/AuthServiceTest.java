package com.fleetmatch.auth.service;

import com.fleetmatch.auth.dto.AuthResponse;
import com.fleetmatch.auth.dto.RegisterRequest;
import com.fleetmatch.auth.dto.VerifyEmailRequest;
import com.fleetmatch.auth.dto.VerifyPhoneRequest;
import com.fleetmatch.auth.entity.VerificationChannel;
import com.fleetmatch.auth.entity.VerificationPurpose;
import com.fleetmatch.audit.service.AuditLogService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
    void loginReturnsTokenAndResetsFailedAttempts() {
        User user = user();
        user.setFailedLoginAttempts(2);
        when(userDetailsService.loadUserByUsername(user.getEmail()))
                .thenReturn(new CustomUserDetails(user));
        when(passwordEncoder.matches("CorrectPass!123", user.getPassword()))
                .thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        AuthResponse response = authService.login(user.getEmail(), "CorrectPass!123");

        assertEquals("jwt-token", response.getToken());
        assertEquals(0, user.getFailedLoginAttempts());
        verify(userRepository).save(user);
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
