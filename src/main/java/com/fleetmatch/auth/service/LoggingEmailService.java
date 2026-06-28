package com.fleetmatch.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LoggingEmailService implements EmailService {

    @Override
    public void sendOtp(String email, String code, String purpose) {
        log.info("Email OTP generated for {} purpose {} code {}", email, purpose, code);
    }

    @Override
    public void sendEmail(String email, String subject, String body) {
        log.info("Email generated for {} subject {} body {}", email, subject, body);
    }
}
