package com.fleetmatch.notification.service;

public interface PushNotificationProvider {

    PushSendResult send(PushMessage message);
}
