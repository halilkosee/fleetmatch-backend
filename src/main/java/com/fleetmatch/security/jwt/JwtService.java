package com.fleetmatch.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.Date;

import com.fleetmatch.user.entity.User;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {

    @Value("${fleetmatch.jwt.secret}")
    private String secret;

    @Value("${fleetmatch.jwt.expiration}")
    private long expiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    public String generateToken(User user) {

        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiration);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("platformRole", user.getPlatformRole().name());

        if (user.getCompany() != null) {
            claims.put("companyType", user.getCompany().getType().name());
            claims.put("companyId", user.getCompany().getId().toString());
        }

        if (user.getCompany() != null) {
            claims.put("companyId", user.getCompany().getId().toString());
        }

        return Jwts.builder()
                .claims(claims)
                .subject(user.getEmail())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {

        Claims claims = extractClaims(token);

        return claims.getSubject();
    }

    public boolean isTokenValidForUser(String token, User user) {
        Claims claims = extractClaims(token);

        if (!user.getEmail().equals(claims.getSubject())) {
            return false;
        }

        if (user.getCredentialsChangedAt() == null || claims.getIssuedAt() == null) {
            return true;
        }

        Date credentialsChangedAt = Date.from(
                user.getCredentialsChangedAt()
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
        );

        return !claims.getIssuedAt().before(credentialsChangedAt);
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
