package com.fleetmatch.auth.service;

import com.fleetmatch.common.exception.BusinessRuleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
@ConditionalOnProperty(
        prefix = "fleetmatch.sms",
        name = "provider",
        havingValue = "twilio"
)
public class TwilioSmsService implements SmsService {

    private final RestClient restClient;
    private final String accountSid;
    private final String authToken;
    private final String from;

    public TwilioSmsService(
            RestClient.Builder restClientBuilder,
            @Value("${fleetmatch.sms.twilio.account-sid:}") String accountSid,
            @Value("${fleetmatch.sms.twilio.auth-token:}") String authToken,
            @Value("${fleetmatch.sms.twilio.from:}") String from
    ) {
        this.restClient = restClientBuilder.build();
        this.accountSid = accountSid;
        this.authToken = authToken;
        this.from = from;
    }

    @Override
    public void sendOtp(String phone, String code, String purpose) {
        if (accountSid == null || accountSid.isBlank() ||
                authToken == null || authToken.isBlank() ||
                from == null || from.isBlank()) {
            throw new BusinessRuleException("Twilio SMS is not configured");
        }

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("To", phone);
        body.add("From", from);
        body.add(
                "Body",
                "Your EasyFleetMatch verification code for " + purpose + " is " + code
        );

        restClient.post()
                .uri("https://api.twilio.com/2010-04-01/Accounts/{accountSid}/Messages.json", accountSid)
                .header("Authorization", "Basic " + basicAuth())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    private String basicAuth() {
        String value = accountSid + ":" + authToken;
        return Base64.getEncoder().encodeToString(
                value.getBytes(StandardCharsets.UTF_8)
        );
    }
}
