package com.fleetmatch.notification.push.service;

import com.fleetmatch.common.exception.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "fleetmatch.push",
        name = "provider",
        havingValue = "fcm"
)
public class FcmPushNotificationProvider implements PushNotificationProvider {

    private static final String TOKEN_URI = "https://oauth2.googleapis.com/token";
    private static final String SCOPE = "https://www.googleapis.com/auth/firebase.messaging";

    private final RestClient.Builder restClientBuilder;

    @Value("${fleetmatch.push.fcm.project-id:}")
    private String projectId;

    @Value("${fleetmatch.push.fcm.client-email:}")
    private String clientEmail;

    @Value("${fleetmatch.push.fcm.private-key:}")
    private String privateKey;

    private volatile CachedAccessToken cachedAccessToken;

    @Override
    public PushSendResult send(PushMessage message) {
        validateConfiguration();

        try {
            FcmResponse response = restClientBuilder.build()
                    .post()
                    .uri("https://fcm.googleapis.com/v1/projects/{projectId}/messages:send", projectId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + accessToken())
                    .body(Map.of("message", fcmMessage(message)))
                    .retrieve()
                    .body(FcmResponse.class);

            return PushSendResult.sent(
                    "fcm",
                    response == null ? null : response.name()
            );
        } catch (RuntimeException ex) {
            return PushSendResult.failed("fcm", ex.getMessage());
        }
    }

    private Map<String, Object> fcmMessage(PushMessage message) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("type", message.type().name());
        data.put("notificationId", message.notificationId().toString());
        data.put("relatedEntityType", nullToEmpty(message.relatedEntityType()));
        data.put("relatedEntityId", uuidToString(message.relatedEntityId()));

        Map<String, Object> fcmMessage = new LinkedHashMap<>();
        fcmMessage.put("token", message.token());
        fcmMessage.put("notification", Map.of(
                "title", message.title(),
                "body", message.body()
        ));
        fcmMessage.put("data", data);
        fcmMessage.put("android", Map.of(
                "priority", "HIGH"
        ));
        fcmMessage.put("apns", Map.of(
                "headers", Map.of("apns-priority", "10"),
                "payload", Map.of(
                        "aps", Map.of(
                                "sound", "default"
                        )
                )
        ));
        return fcmMessage;
    }

    private String accessToken() {
        CachedAccessToken token = cachedAccessToken;
        if (token != null && token.expiresAt().isAfter(Instant.now().plusSeconds(60))) {
            return token.value();
        }

        synchronized (this) {
            token = cachedAccessToken;
            if (token != null && token.expiresAt().isAfter(Instant.now().plusSeconds(60))) {
                return token.value();
            }

            TokenResponse response = restClientBuilder.build()
                    .post()
                    .uri(TOKEN_URI)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body("grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion=" + signedJwt())
                    .retrieve()
                    .body(TokenResponse.class);

            if (response == null || response.access_token() == null || response.access_token().isBlank()) {
                throw new BusinessRuleException("FCM access token was not returned");
            }

            cachedAccessToken = new CachedAccessToken(
                    response.access_token(),
                    Instant.now().plusSeconds(response.expires_in() == null ? 3000 : response.expires_in())
            );
            return cachedAccessToken.value();
        }
    }

    private String signedJwt() {
        Instant now = Instant.now();
        String header = base64Url("{\"alg\":\"RS256\",\"typ\":\"JWT\"}");
        String claims = base64Url("""
                {"iss":"%s","scope":"%s","aud":"%s","iat":%d,"exp":%d}
                """.formatted(clientEmail, SCOPE, TOKEN_URI, now.getEpochSecond(), now.plusSeconds(3600).getEpochSecond()).trim());
        String signingInput = header + "." + claims;
        return signingInput + "." + sign(signingInput);
    }

    private String sign(String signingInput) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(parsePrivateKey());
            signature.update(signingInput.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(signature.sign());
        } catch (Exception ex) {
            throw new BusinessRuleException("FCM service account private key is invalid");
        }
    }

    private PrivateKey parsePrivateKey() throws Exception {
        String normalized = privateKey
                .replace("\\n", "\n")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(normalized);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
        return KeyFactory.getInstance("RSA").generatePrivate(keySpec);
    }

    private String base64Url(String value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private void validateConfiguration() {
        if (isBlank(projectId) || isBlank(clientEmail) || isBlank(privateKey)) {
            throw new BusinessRuleException("FCM push provider is not configured");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String uuidToString(UUID value) {
        return value == null ? "" : value.toString();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private record CachedAccessToken(String value, Instant expiresAt) {
    }

    private record TokenResponse(String access_token, String token_type, Integer expires_in) {
    }

    private record FcmResponse(String name) {
    }
}
