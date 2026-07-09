package com.fleetmatch.auth.service;

import com.fleetmatch.auth.entity.UserVerificationCode;
import com.fleetmatch.auth.entity.VerificationChannel;
import com.fleetmatch.auth.entity.VerificationPurpose;
import com.fleetmatch.auth.repository.UserVerificationCodeRepository;
import com.fleetmatch.user.entity.User;
import com.fleetmatch.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificationCodeServiceTest {

    @Mock
    private UserVerificationCodeRepository codeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private SmsService smsService;

    @InjectMocks
    private VerificationCodeService verificationCodeService;

    @Test
    void devEnvironmentUsesConfiguredFixedCode() {
        ReflectionTestUtils.setField(verificationCodeService, "environment", "DEV");
        ReflectionTestUtils.setField(verificationCodeService, "fixedCode", "123456");
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-code");
        User user = user();

        var response = verificationCodeService.createCode(
                user,
                VerificationPurpose.EMAIL_VERIFICATION,
                VerificationChannel.EMAIL,
                user.getEmail()
        );

        ArgumentCaptor<UserVerificationCode> captor =
                ArgumentCaptor.forClass(UserVerificationCode.class);
        verify(codeRepository).save(captor.capture());
        verify(emailService).sendOtp(
                user.getEmail(),
                "123456",
                VerificationPurpose.EMAIL_VERIFICATION.name()
        );
        verify(passwordEncoder).encode("123456");
        assertEquals("hashed-code", captor.getValue().getCodeHash());
        assertEquals("123456", response.getDebugCode());
    }

    private User user() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("driver@example.test");
        return user;
    }
}
