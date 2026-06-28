package com.fleetmatch.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(
        prefix = "fleetmatch.sms",
        name = "provider",
        havingValue = "log",
        matchIfMissing = true
)
public class LoggingSmsService implements SmsService {

    @Override
    public void sendOtp(String phone, String code, String purpose) {
        log.info("SMS OTP generated for {} purpose {} code {}", phone, purpose, code);
    }
}
