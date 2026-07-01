package com.fleetmatch.auth.controller;

import com.fleetmatch.auth.dto.AuthResponse;
import com.fleetmatch.auth.dto.ForgotPasswordRequest;
import com.fleetmatch.auth.dto.LoginRequest;
import com.fleetmatch.auth.dto.LogoutRequest;
import com.fleetmatch.auth.dto.RegisterRequest;
import com.fleetmatch.auth.dto.RefreshTokenRequest;
import com.fleetmatch.auth.dto.ResetPasswordRequest;
import com.fleetmatch.auth.dto.ResendEmailCodeRequest;
import com.fleetmatch.auth.dto.ResendPhoneCodeRequest;
import com.fleetmatch.auth.dto.VerificationCodeResponse;
import com.fleetmatch.auth.dto.VerifyEmailRequest;
import com.fleetmatch.auth.dto.VerifyPhoneRequest;
import com.fleetmatch.auth.service.AuthService;
import com.fleetmatch.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new company owner")
    public AuthResponse register(
            @Valid @RequestBody RegisterRequest request
    ) {

        authService.register(request);

        return new AuthResponse(
                "Registration submitted successfully"
        );
    }

    @PostMapping("/login")
    @Operation(summary = "Login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {

        return authService.login(
                request.getEmail(),
                request.getPassword(),
                httpRequest.getHeader("User-Agent"),
                clientIp(httpRequest)
        );
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    public AuthResponse refresh(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest
    ) {
        return authService.refresh(
                request.getRefreshToken(),
                httpRequest.getHeader("User-Agent"),
                clientIp(httpRequest)
        );
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout current refresh token")
    public AuthResponse logout(
            @Valid @RequestBody LogoutRequest request
    ) {
        authService.logout(request.getRefreshToken());
        return new AuthResponse("Logged out");
    }

    @PostMapping("/logout-all")
    @Operation(summary = "Logout all sessions for the authenticated user")
    public AuthResponse logoutAll(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        authService.logoutAll(currentUser);
        return new AuthResponse("All sessions logged out");
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify email OTP")
    public AuthResponse verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request
    ) {
        authService.verifyEmail(request);
        return new AuthResponse("Email verified");
    }

    @PostMapping("/resend-email-code")
    @Operation(summary = "Resend email verification OTP")
    public VerificationCodeResponse resendEmailCode(
            @Valid @RequestBody ResendEmailCodeRequest request
    ) {
        return authService.resendEmailCode(request);
    }

    @PostMapping("/verify-phone")
    @Operation(summary = "Verify phone OTP")
    public AuthResponse verifyPhone(
            @Valid @RequestBody VerifyPhoneRequest request
    ) {
        authService.verifyPhone(request);
        return new AuthResponse("Phone verified");
    }

    @PostMapping("/resend-phone-code")
    @Operation(summary = "Resend phone verification OTP")
    public VerificationCodeResponse resendPhoneCode(
            @Valid @RequestBody ResendPhoneCodeRequest request
    ) {
        return authService.resendPhoneCode(request);
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset OTP")
    public VerificationCodeResponse forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        return authService.forgotPassword(request);
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password with OTP")
    public AuthResponse resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        authService.resetPassword(request);
        return new AuthResponse("Password reset");
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
