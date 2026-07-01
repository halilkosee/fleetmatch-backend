package com.fleetmatch.notification.push.service;

public interface PushNotificationProvider {

    PushSendResult send(PushMessage message);
}
