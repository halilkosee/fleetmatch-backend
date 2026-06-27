package com.fleetmatch.user.controller;

import com.fleetmatch.auth.dto.VerificationCodeResponse;
import com.fleetmatch.security.user.CustomUserDetails;
import com.fleetmatch.user.dto.*;
import com.fleetmatch.user.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Account Settings")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final AccountService accountService;

    @GetMapping("/me")
    @Operation(summary = "Get current user account")
    public UserAccountResponse me(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return accountService.getMe(userDetails);
    }

    @PutMapping("/me/profile")
    @Operation(summary = "Update current user profile")
    public UserAccountResponse updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        return accountService.updateProfile(userDetails, request);
    }

    @PutMapping("/me/password")
    @Operation(summary = "Change current user password")
    public void changePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        accountService.changePassword(userDetails, request);
    }

    @PostMapping("/me/change-email/request")
    @Operation(summary = "Request email change OTP")
    public VerificationCodeResponse requestEmailChange(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ChangeEmailRequest request
    ) {
        return accountService.requestEmailChange(userDetails, request);
    }

    @PostMapping("/me/change-email/verify")
    @Operation(summary = "Verify email change OTP")
    public UserAccountResponse verifyEmailChange(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody VerifyEmailChangeRequest request
    ) {
        return accountService.verifyEmailChange(userDetails, request);
    }

    @PostMapping("/me/change-phone/request")
    @Operation(summary = "Request phone change OTP")
    public VerificationCodeResponse requestPhoneChange(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ChangePhoneRequest request
    ) {
        return accountService.requestPhoneChange(userDetails, request);
    }

    @PostMapping("/me/change-phone/verify")
    @Operation(summary = "Verify phone change OTP")
    public UserAccountResponse verifyPhoneChange(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody VerifyPhoneChangeRequest request
    ) {
        return accountService.verifyPhoneChange(userDetails, request);
    }
}
