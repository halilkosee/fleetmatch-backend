package com.fleetmatch.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetmatch.common.exception.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimitingFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        HttpServletRequest activeRequest = request;
        Rule rule = resolveRule(activeRequest);
        if (rule == null) {
            filterChain.doFilter(activeRequest, response);
            return;
        }

        String bodyIdentity = "";
        if (rule.includeRequestBody()) {
            CachedBodyRequest cachedRequest = new CachedBodyRequest(activeRequest);
            activeRequest = cachedRequest;
            bodyIdentity = sha256(cachedRequest.body());
        }

        String key = buildKey(rule, activeRequest, bodyIdentity);
        Window window = windows.compute(key, (ignored, existing) -> {
            long now = System.currentTimeMillis();
            if (existing == null || now > existing.resetAt) {
                return new Window(1, now + rule.windowMillis);
            }
            existing.count++;
            return existing;
        });

        if (window.count > rule.limit) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            objectMapper.writeValue(
                    response.getWriter(),
                    new ApiError(
                            LocalDateTime.now(),
                            HttpStatus.TOO_MANY_REQUESTS.value(),
                            "RATE_LIMITED",
                            "Too many requests",
                            activeRequest.getRequestURI(),
                            List.of()
                    )
            );
            return;
        }

        filterChain.doFilter(activeRequest, response);
    }

    private Rule resolveRule(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return null;
        }

        String path = request.getRequestURI();

        if ("/api/auth/login".equals(path)) {
            return new Rule("login", 5, 60_000, false, false);
        }
        if ("/api/auth/register".equals(path)) {
            return new Rule("register", 5, 60_000, false, false);
        }
        if (path.contains("resend") || path.contains("change-email/request") ||
                path.contains("change-phone/request") || path.contains("forgot-password")) {
            return new Rule("otp", 3, 600_000, false, true);
        }
        if (path.matches("/api/conversations/.+/messages")) {
            return new Rule("message", 30, 60_000, true, false);
        }

        return null;
    }

    private String buildKey(Rule rule, HttpServletRequest request, String bodyIdentity) {
        String identity = request.getRemoteAddr();

        if (rule.preferAuthenticatedUser()) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null &&
                    authentication.isAuthenticated() &&
                    authentication.getName() != null &&
                    !"anonymousUser".equals(authentication.getName())) {
                identity = authentication.getName();
            }
        }

        return rule.name + ":" + identity + ":" + request.getRequestURI() + ":" + bodyIdentity;
    }

    private String sha256(byte[] value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private record Rule(
            String name,
            int limit,
            long windowMillis,
            boolean preferAuthenticatedUser,
            boolean includeRequestBody
    ) {
    }

    private static class Window {
        private int count;
        private final long resetAt;

        private Window(int count, long resetAt) {
            this.count = count;
            this.resetAt = resetAt;
        }
    }

    private static class CachedBodyRequest extends HttpServletRequestWrapper {

        private final byte[] body;

        private CachedBodyRequest(HttpServletRequest request) throws IOException {
            super(request);
            this.body = request.getInputStream().readAllBytes();
        }

        private byte[] body() {
            return body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(body);

            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return inputStream.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    // Synchronous servlet reads are enough for JSON API rate limiting.
                }

                @Override
                public int read() {
                    return inputStream.read();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(
                    new InputStreamReader(getInputStream(), StandardCharsets.UTF_8)
            );
        }
    }
}
