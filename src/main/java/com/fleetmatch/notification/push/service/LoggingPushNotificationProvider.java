package com.fleetmatch.notification.push.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(
        prefix = "fleetmatch.push",
        name = "provider",
        havingValue = "log",
        matchIfMissing = true
)
public class LoggingPushNotificationProvider implements PushNotificationProvider {

    @Override
    public PushSendResult send(PushMessage message) {
        log.info(
                "Push notification generated for token {} title {} body {}",
                maskToken(message.token()),
                message.title(),
                message.body()
        );
        return PushSendResult.sent("log", null);
    }

    private String maskToken(String token) {
        if (token == null || token.length() < 12) {
            return "****";
        }
        return token.substring(0, 6) + "..." + token.substring(token.length() - 4);
    }
}
