package com.fleetmatch.user.service;

import com.fleetmatch.auth.dto.VerificationCodeResponse;
import com.fleetmatch.auth.entity.VerificationChannel;
import com.fleetmatch.auth.entity.VerificationPurpose;
import com.fleetmatch.auth.service.PasswordPolicyService;
import com.fleetmatch.auth.service.VerificationCodeService;
import com.fleetmatch.audit.entity.AuditAction;
import com.fleetmatch.audit.service.AuditLogService;
import com.fleetmatch.common.exception.BusinessRuleException;
import com.fleetmatch.common.exception.ResourceAlreadyExistsException;
import com.fleetmatch.common.exception.ResourceNotFoundException;
import com.fleetmatch.security.user.CustomUserDetails;
import com.fleetmatch.user.dto.*;
import com.fleetmatch.user.entity.User;
import com.fleetmatch.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyService passwordPolicyService;
    private final VerificationCodeService verificationCodeService;
    private final AuditLogService auditLogService;

    public UserAccountResponse getMe(CustomUserDetails currentUser) {
        return toResponse(getCurrentUser(currentUser));
    }

    @Transactional
    public UserAccountResponse updateProfile(
            CustomUserDetails currentUser,
            UpdateUserProfileRequest request
    ) {
        User user = getCurrentUser(currentUser);
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void changePassword(
            CustomUserDetails currentUser,
            ChangePasswordRequest request
    ) {
        User user = getCurrentUser(currentUser);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        passwordPolicyService.validate(request.getNewPassword(), user.getEmail());
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setCredentialsChangedAt(LocalDateTime.now());
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
        auditLogService.log(user, AuditAction.PASSWORD_CHANGED, "USER", user.getId(), "Password changed");
    }

    @Transactional
    public VerificationCodeResponse requestEmailChange(
            CustomUserDetails currentUser,
            ChangeEmailRequest request
    ) {
        User user = getCurrentUser(currentUser);

        if (userRepository.existsByEmail(request.getNewEmail())) {
            throw new ResourceAlreadyExistsException("Email already exists");
        }

        return verificationCodeService.createCode(
                user,
                VerificationPurpose.EMAIL_CHANGE,
                VerificationChannel.EMAIL,
                request.getNewEmail()
        );
    }

    @Transactional
    public UserAccountResponse verifyEmailChange(
            CustomUserDetails currentUser,
            VerifyEmailChangeRequest request
    ) {
        User user = getCurrentUser(currentUser);

        if (userRepository.existsByEmail(request.getNewEmail())) {
            throw new ResourceAlreadyExistsException("Email already exists");
        }

        verificationCodeService.verifyCode(
                user.getId(),
                VerificationPurpose.EMAIL_CHANGE,
                VerificationChannel.EMAIL,
                request.getNewEmail(),
                request.getCode()
        );

        user.setEmail(request.getNewEmail());
        user.setEmailVerified(true);
        user.setEmailVerifiedAt(LocalDateTime.now());
        user.setCredentialsChangedAt(LocalDateTime.now());

        User saved = userRepository.save(user);
        auditLogService.log(saved, AuditAction.EMAIL_CHANGED, "USER", saved.getId(), "Email changed");
        return toResponse(saved);
    }

    @Transactional
    public VerificationCodeResponse requestPhoneChange(
            CustomUserDetails currentUser,
            ChangePhoneRequest request
    ) {
        User user = getCurrentUser(currentUser);
        validatePhoneAvailable(request.getNewPhone());

        return verificationCodeService.createCode(
                user,
                VerificationPurpose.PHONE_CHANGE,
                VerificationChannel.PHONE,
                request.getNewPhone()
        );
    }

    @Transactional
    public UserAccountResponse verifyPhoneChange(
            CustomUserDetails currentUser,
            VerifyPhoneChangeRequest request
    ) {
        User user = getCurrentUser(currentUser);
        validatePhoneAvailable(request.getNewPhone());

        verificationCodeService.verifyCode(
                user.getId(),
                VerificationPurpose.PHONE_CHANGE,
                VerificationChannel.PHONE,
                request.getNewPhone(),
                request.getCode()
        );

        user.setPhone(request.getNewPhone());
        user.setPhoneVerified(true);
        user.setPhoneVerifiedAt(LocalDateTime.now());

        User saved = userRepository.save(user);
        auditLogService.log(saved, AuditAction.PHONE_CHANGED, "USER", saved.getId(), "Phone changed");
        return toResponse(saved);
    }

    private User getCurrentUser(CustomUserDetails currentUser) {
        return userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private void validatePhoneAvailable(String phone) {
        if (phone != null &&
                !phone.isBlank() &&
                userRepository.existsByPhone(phone)) {
            throw new ResourceAlreadyExistsException("Phone already exists");
        }

        if (phone == null || phone.isBlank()) {
            throw new BusinessRuleException("Phone is required");
        }
    }

    private UserAccountResponse toResponse(User user) {
        return UserAccountResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .emailVerified(user.isEmailVerified())
                .emailVerifiedAt(user.getEmailVerifiedAt())
                .phone(user.getPhone())
                .phoneVerified(user.isPhoneVerified())
                .phoneVerifiedAt(user.getPhoneVerifiedAt())
                .platformRole(user.getPlatformRole())
                .companyUserRole(user.getCompanyUserRole())
                .status(user.getStatus())
                .companyId(user.getCompany() == null ? null : user.getCompany().getId())
                .companyName(user.getCompany() == null ? null : user.getCompany().getLegalName())
                .companyType(user.getCompany() == null ? null : user.getCompany().getType())
                .build();
    }
}
