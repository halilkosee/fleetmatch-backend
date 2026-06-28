package com.fleetmatch.auth.service;

import com.fleetmatch.common.exception.BusinessRuleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
@ConditionalOnProperty(
        prefix = "fleetmatch.sms",
        name = "provider",
        havingValue = "webhook"
)
public class WebhookSmsService implements SmsService {

    private final RestClient restClient;
    private final String webhookUrl;
    private final String bearerToken;
    private final String from;

    public WebhookSmsService(
            RestClient.Builder restClientBuilder,
            @Value("${fleetmatch.sms.webhook-url:}") String webhookUrl,
            @Value("${fleetmatch.sms.token:}") String bearerToken,
            @Value("${fleetmatch.sms.from:EasyFleetMatch}") String from
    ) {
        this.restClient = restClientBuilder.build();
        this.webhookUrl = webhookUrl;
        this.bearerToken = bearerToken;
        this.from = from;
    }

    @Override
    public void sendOtp(String phone, String code, String purpose) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            throw new BusinessRuleException("SMS webhook URL is not configured");
        }

        RestClient.RequestBodySpec request = restClient
                .post()
                .uri(webhookUrl)
                .contentType(MediaType.APPLICATION_JSON);

        if (bearerToken != null && !bearerToken.isBlank()) {
            request.header("Authorization", "Bearer " + bearerToken);
        }

        request.body(Map.of(
                        "to", phone,
                        "from", from,
                        "purpose", purpose,
                        "message", "Your EasyFleetMatch verification code is " + code
                ))
                .retrieve()
                .toBodilessEntity();
    }
}
