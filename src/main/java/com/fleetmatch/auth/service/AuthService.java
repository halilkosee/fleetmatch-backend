package com.fleetmatch.auth.service;

import com.fleetmatch.auth.dto.AuthResponse;
import com.fleetmatch.auth.dto.ForgotPasswordRequest;
import com.fleetmatch.auth.dto.RegisterRequest;
import com.fleetmatch.auth.dto.ResetPasswordRequest;
import com.fleetmatch.auth.dto.ResendEmailCodeRequest;
import com.fleetmatch.auth.dto.ResendPhoneCodeRequest;
import com.fleetmatch.auth.dto.VerificationCodeResponse;
import com.fleetmatch.auth.dto.VerifyEmailRequest;
import com.fleetmatch.auth.dto.VerifyPhoneRequest;
import com.fleetmatch.auth.entity.VerificationChannel;
import com.fleetmatch.auth.entity.VerificationPurpose;
import com.fleetmatch.audit.entity.AuditAction;
import com.fleetmatch.audit.service.AuditLogService;
import com.fleetmatch.common.exception.AccountNotActiveException;
import com.fleetmatch.common.exception.BusinessRuleException;
import com.fleetmatch.common.exception.ResourceNotFoundException;
import com.fleetmatch.common.exception.ResourceAlreadyExistsException;
import com.fleetmatch.company.entity.Company;
import com.fleetmatch.company.repository.CompanyRepository;
import com.fleetmatch.subscription.service.SubscriptionService;
import com.fleetmatch.user.entity.CompanyUserRole;
import com.fleetmatch.user.entity.PlatformRole;
import com.fleetmatch.user.entity.User;
import com.fleetmatch.user.entity.UserStatus;
import com.fleetmatch.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fleetmatch.security.jwt.JwtService;
import com.fleetmatch.security.user.CustomUserDetails;
import com.fleetmatch.security.user.CustomUserDetailsService;
import org.springframework.security.authentication.BadCredentialsException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 15;

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final SubscriptionService
            subscriptionService;
    private final PasswordPolicyService passwordPolicyService;
    private final VerificationCodeService verificationCodeService;
    private final AuditLogService auditLogService;

    @Transactional
    public void register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceAlreadyExistsException(
                    "Email already exists"
            );
        }

        if (request.getPhone() != null &&
                !request.getPhone().isBlank() &&
                userRepository.existsByPhone(request.getPhone())) {
            throw new ResourceAlreadyExistsException(
                    "Phone already exists"
            );
        }

        passwordPolicyService.validate(request.getPassword());

        Company company = new Company();
        company.setLegalName(request.getCompanyLegalName());
        company.setDbaName(request.getCompanyDbaName());
        company.setEmail(request.getCompanyEmail());
        company.setPhone(request.getCompanyPhone());
        company.setType(request.getCompanyType());

        company = companyRepository.save(company);
        subscriptionService.assignFreePlan(company);

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setCredentialsChangedAt(LocalDateTime.now());
        user.setPlatformRole(PlatformRole.USER);
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        user.setCompany(company);
        user.setCompanyUserRole(
                CompanyUserRole.OWNER
        );

        userRepository.save(user);
        auditLogService.log(
                user,
                AuditAction.USER_REGISTERED,
                "USER",
                user.getId(),
                "User registered"
        );

        verificationCodeService.createCode(
                user,
                VerificationPurpose.EMAIL_VERIFICATION,
                VerificationChannel.EMAIL,
                user.getEmail()
        );

        if (user.getPhone() != null && !user.getPhone().isBlank()) {
            verificationCodeService.createCode(
                    user,
                    VerificationPurpose.PHONE_VERIFICATION,
                    VerificationChannel.PHONE,
                    user.getPhone()
            );
        }
    }

    @Transactional
    public AuthResponse login(String email, String password) {

        CustomUserDetails userDetails =
                (CustomUserDetails) userDetailsService.loadUserByUsername(email);
        User user = userDetails.getUser();

        if (user.getLockedUntil() != null &&
                user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new AccountNotActiveException(
                    "Account is temporarily locked due to failed login attempts"
            );
        }

        if (!passwordEncoder.matches(
                password,
                userDetails.getPassword()
        )) {
            recordFailedLogin(user);
            throw new BadCredentialsException("Invalid credentials");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccountNotActiveException(
                    "Account is pending verification"
            );
        }

        resetFailedLogin(user);

        String token =
                jwtService.generateToken(user);

        return new AuthResponse(token);
    }

    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        verificationCodeService.verifyCode(
                user.getId(),
                VerificationPurpose.EMAIL_VERIFICATION,
                VerificationChannel.EMAIL,
                user.getEmail(),
                request.getCode()
        );

        user.setEmailVerified(true);
        user.setEmailVerifiedAt(LocalDateTime.now());
        userRepository.save(user);
        auditLogService.log(user, AuditAction.EMAIL_VERIFIED, "USER", user.getId(), "Email verified");
    }

    @Transactional
    public VerificationCodeResponse resendEmailCode(ResendEmailCodeRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return verificationCodeService.createCode(
                user,
                VerificationPurpose.EMAIL_VERIFICATION,
                VerificationChannel.EMAIL,
                user.getEmail()
        );
    }

    @Transactional
    public void verifyPhone(VerifyPhoneRequest request) {
        User user = userRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        verificationCodeService.verifyCode(
                user.getId(),
                VerificationPurpose.PHONE_VERIFICATION,
                VerificationChannel.PHONE,
                user.getPhone(),
                request.getCode()
        );

        user.setPhoneVerified(true);
        user.setPhoneVerifiedAt(LocalDateTime.now());
        userRepository.save(user);
        auditLogService.log(user, AuditAction.PHONE_VERIFIED, "USER", user.getId(), "Phone verified");
    }

    @Transactional
    public VerificationCodeResponse resendPhoneCode(ResendPhoneCodeRequest request) {
        User user = userRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return verificationCodeService.createCode(
                user,
                VerificationPurpose.PHONE_VERIFICATION,
                VerificationChannel.PHONE,
                user.getPhone()
        );
    }

    @Transactional
    public VerificationCodeResponse forgotPassword(ForgotPasswordRequest request) {
        return userRepository.findByEmail(request.getEmail())
                .map(user -> verificationCodeService.createCode(
                        user,
                        VerificationPurpose.PASSWORD_RESET,
                        VerificationChannel.EMAIL,
                        user.getEmail()
                ))
                .orElseGet(() -> new VerificationCodeResponse(
                        "If the email exists, a reset code has been sent",
                        null
                ));
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        passwordPolicyService.validate(request.getNewPassword());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessRuleException("Invalid or expired verification code"));

        verificationCodeService.verifyCode(
                user.getId(),
                VerificationPurpose.PASSWORD_RESET,
                VerificationChannel.EMAIL,
                user.getEmail(),
                request.getCode()
        );

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setCredentialsChangedAt(LocalDateTime.now());
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
        auditLogService.log(user, AuditAction.PASSWORD_RESET, "USER", user.getId(), "Password reset");
    }

    private void recordFailedLogin(User user) {
        user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);

        if (user.getFailedLoginAttempts() >= MAX_FAILED_LOGIN_ATTEMPTS) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
        }

        userRepository.save(user);
    }

    private void resetFailedLogin(User user) {
        if (user.getFailedLoginAttempts() == 0 && user.getLockedUntil() == null) {
            return;
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
    }
}
