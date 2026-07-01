package com.fleetmatch.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    private String token;

    private String refreshToken;

    private String tokenType;

    private Long expiresInMs;

    public AuthResponse(String token) {
        this.token = token;
    }

    public AuthResponse(
            String token,
            String refreshToken,
            String tokenType,
            Long expiresInMs
    ) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.tokenType = tokenType;
        this.expiresInMs = expiresInMs;
    }

    public static AuthResponse authenticated(
            String token,
            String refreshToken,
            Long expiresInMs
    ) {
        return new AuthResponse(token, refreshToken, "Bearer", expiresInMs);
    }
}
