package com.fleetmatch.auth.controller;

import com.fleetmatch.auth.dto.AuthResponse;
import com.fleetmatch.auth.dto.LoginRequest;
import com.fleetmatch.auth.dto.RegisterRequest;
import com.fleetmatch.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public AuthResponse register(
            @Valid @RequestBody RegisterRequest request
    ) {

        authService.register(request);

        return new AuthResponse(
                "Registration submitted successfully"
        );
    }

    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest request
    ) {

        return authService.login(
                request.getEmail(),
                request.getPassword()
        );
    }
}
