package com.fleetmatch.auth.controller;

import com.fleetmatch.auth.dto.AuthResponse;
import com.fleetmatch.auth.dto.ForgotPasswordRequest;
import com.fleetmatch.auth.dto.LoginRequest;
import com.fleetmatch.auth.dto.RegisterRequest;
import com.fleetmatch.auth.dto.ResetPasswordRequest;
import com.fleetmatch.auth.dto.ResendEmailCodeRequest;
import com.fleetmatch.auth.dto.ResendPhoneCodeRequest;
import com.fleetmatch.auth.dto.VerificationCodeResponse;
import com.fleetmatch.auth.dto.VerifyEmailRequest;
import com.fleetmatch.auth.dto.VerifyPhoneRequest;
import com.fleetmatch.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;

    private final PasswordEncoder passwordEncoder;

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
            @Valid @RequestBody LoginRequest request
    ) {

        return authService.login(
                request.getEmail(),
                request.getPassword()
        );
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

    @GetMapping("/debug/password")
    public String debugPassword() {
        return passwordEncoder.encode("123456");
    }
}
