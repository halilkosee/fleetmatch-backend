package com.fleetmatch.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ProductionReadinessValidator {

    private final String environment;
    private final String mailProvider;
    private final String mailHost;
    private final String mailFrom;
    private final String smsProvider;
    private final String smsWebhookUrl;
    private final String twilioAccountSid;
    private final String twilioAuthToken;
    private final String twilioFrom;
    private final String pushProvider;
    private final String fcmProjectId;
    private final String fcmClientEmail;
    private final String fcmPrivateKey;
    private final String rateLimitStore;
    private final String jwtSecret;

    public ProductionReadinessValidator(
            @Value("${fleetmatch.environment:LOCAL}") String environment,
            @Value("${fleetmatch.mail.provider:log}") String mailProvider,
            @Value("${fleetmatch.mail.host:}") String mailHost,
            @Value("${fleetmatch.mail.from:}") String mailFrom,
            @Value("${fleetmatch.sms.provider:log}") String smsProvider,
            @Value("${fleetmatch.sms.webhook-url:}") String smsWebhookUrl,
            @Value("${fleetmatch.sms.twilio.account-sid:}") String twilioAccountSid,
            @Value("${fleetmatch.sms.twilio.auth-token:}") String twilioAuthToken,
            @Value("${fleetmatch.sms.twilio.from:}") String twilioFrom,
            @Value("${fleetmatch.push.provider:log}") String pushProvider,
            @Value("${fleetmatch.push.fcm.project-id:}") String fcmProjectId,
            @Value("${fleetmatch.push.fcm.client-email:}") String fcmClientEmail,
            @Value("${fleetmatch.push.fcm.private-key:}") String fcmPrivateKey,
            @Value("${fleetmatch.rate-limit.store:memory}") String rateLimitStore,
            @Value("${fleetmatch.jwt.secret:}") String jwtSecret
    ) {
        this.environment = environment;
        this.mailProvider = mailProvider;
        this.mailHost = mailHost;
        this.mailFrom = mailFrom;
        this.smsProvider = smsProvider;
        this.smsWebhookUrl = smsWebhookUrl;
        this.twilioAccountSid = twilioAccountSid;
        this.twilioAuthToken = twilioAuthToken;
        this.twilioFrom = twilioFrom;
        this.pushProvider = pushProvider;
        this.fcmProjectId = fcmProjectId;
        this.fcmClientEmail = fcmClientEmail;
        this.fcmPrivateKey = fcmPrivateKey;
        this.rateLimitStore = rateLimitStore;
        this.jwtSecret = jwtSecret;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validate() {
        if (!"PROD".equalsIgnoreCase(environment)) {
            return;
        }

        if ("log".equalsIgnoreCase(mailProvider)) {
            throw new IllegalStateException("PROD requires a real mail provider");
        }
        if ("smtp".equalsIgnoreCase(mailProvider) &&
                (isBlank(mailHost) || isBlank(mailFrom))) {
            throw new IllegalStateException("PROD SMTP mail host and from address are required");
        }
        if ("log".equalsIgnoreCase(smsProvider)) {
            throw new IllegalStateException("PROD requires a real SMS provider");
        }
        if ("webhook".equalsIgnoreCase(smsProvider) && isBlank(smsWebhookUrl)) {
            throw new IllegalStateException("PROD SMS webhook URL is required");
        }
        if ("twilio".equalsIgnoreCase(smsProvider) &&
                (isBlank(twilioAccountSid) || isBlank(twilioAuthToken) || isBlank(twilioFrom))) {
            throw new IllegalStateException("PROD Twilio credentials are required");
        }
        if ("log".equalsIgnoreCase(pushProvider)) {
            throw new IllegalStateException("PROD requires a real push notification provider");
        }
        if ("fcm".equalsIgnoreCase(pushProvider) &&
                (isBlank(fcmProjectId) || isBlank(fcmClientEmail) || isBlank(fcmPrivateKey))) {
            throw new IllegalStateException("PROD FCM credentials are required");
        }
        if (!"redis".equalsIgnoreCase(rateLimitStore)) {
            throw new IllegalStateException("PROD requires Redis-backed rate limiting");
        }
        if (jwtSecret == null || jwtSecret.length() < 32) {
            throw new IllegalStateException("PROD requires a strong JWT secret");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
