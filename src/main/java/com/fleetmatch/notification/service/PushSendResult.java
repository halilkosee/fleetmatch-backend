package com.fleetmatch.notification.service;

public record PushSendResult(
        boolean successful,
        String provider,
        String providerMessageId,
        String errorMessage
) {

    public static PushSendResult sent(String provider, String providerMessageId) {
        return new PushSendResult(true, provider, providerMessageId, null);
    }

    public static PushSendResult failed(String provider, String errorMessage) {
        return new PushSendResult(false, provider, null, errorMessage);
    }
}
