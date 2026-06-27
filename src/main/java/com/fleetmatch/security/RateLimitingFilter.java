package com.fleetmatch.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetmatch.common.exception.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
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

        Rule rule = resolveRule(request);
        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = buildKey(rule, request);
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
                            request.getRequestURI(),
                            List.of()
                    )
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    private Rule resolveRule(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return null;
        }

        String path = request.getRequestURI();

        if ("/api/auth/login".equals(path)) {
            return new Rule("login", 5, 60_000, false);
        }
        if ("/api/auth/register".equals(path)) {
            return new Rule("register", 5, 60_000, false);
        }
        if (path.contains("resend") || path.contains("change-email/request") ||
                path.contains("change-phone/request") || path.contains("forgot-password")) {
            return new Rule("otp", 3, 600_000, false);
        }
        if (path.matches("/api/conversations/.+/messages")) {
            return new Rule("message", 30, 60_000, true);
        }

        return null;
    }

    private String buildKey(Rule rule, HttpServletRequest request) {
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

        return rule.name + ":" + identity + ":" + request.getRequestURI();
    }

    private record Rule(String name, int limit, long windowMillis, boolean preferAuthenticatedUser) {
    }

    private static class Window {
        private int count;
        private final long resetAt;

        private Window(int count, long resetAt) {
            this.count = count;
            this.resetAt = resetAt;
        }
    }
}
