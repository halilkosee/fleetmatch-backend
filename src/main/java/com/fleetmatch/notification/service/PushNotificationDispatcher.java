package com.fleetmatch.notification.service;

import com.fleetmatch.notification.entity.Notification;
import com.fleetmatch.notification.entity.NotificationDelivery;
import com.fleetmatch.notification.entity.NotificationDeliveryChannel;
import com.fleetmatch.notification.entity.NotificationDeliveryStatus;
import com.fleetmatch.notification.entity.PushDeviceToken;
import com.fleetmatch.notification.repository.NotificationDeliveryRepository;
import com.fleetmatch.notification.repository.PushDeviceTokenRepository;
import com.fleetmatch.user.entity.User;
import com.fleetmatch.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PushNotificationDispatcher {

    private final PushDeviceTokenRepository pushDeviceTokenRepository;
    private final NotificationDeliveryRepository notificationDeliveryRepository;
    private final UserRepository userRepository;
    private final PushNotificationProvider pushNotificationProvider;

    @Transactional
    public void dispatchToUser(Notification notification, User user) {
        List<PushDeviceToken> tokens =
                pushDeviceTokenRepository.findByUserIdAndActiveTrue(user.getId());
        dispatch(notification, user, tokens);
    }

    @Transactional
    public void dispatchToCompany(Notification notification) {
        if (notification.getCompany() == null) {
            return;
        }

        List<User> users = userRepository.findByCompanyId(notification.getCompany().getId());
        if (users.isEmpty()) {
            return;
        }

        List<PushDeviceToken> tokens = pushDeviceTokenRepository.findByUserIdInAndActiveTrue(
                users.stream().map(User::getId).toList()
        );

        for (User user : users) {
            List<PushDeviceToken> userTokens = tokens.stream()
                    .filter(token -> token.getUser().getId().equals(user.getId()))
                    .toList();
            dispatch(notification, user, userTokens);
        }
    }

    private void dispatch(
            Notification notification,
            User user,
            List<PushDeviceToken> tokens
    ) {
        if (tokens.isEmpty()) {
            saveDelivery(
                    notification,
                    user,
                    null,
                    NotificationDeliveryStatus.SKIPPED,
                    "none",
                    null,
                    "No active push device token"
            );
            return;
        }

        for (PushDeviceToken token : tokens) {
            PushSendResult result;
            try {
                result = pushNotificationProvider.send(new PushMessage(
                        token.getToken(),
                        notification.getTitle(),
                        notification.getMessage(),
                        notification.getType(),
                        notification.getRelatedEntityType(),
                        notification.getRelatedEntityId(),
                        notification.getId()
                ));
            } catch (RuntimeException ex) {
                result = PushSendResult.failed("unknown", ex.getMessage());
            }

            token.setLastUsedAt(LocalDateTime.now());
            pushDeviceTokenRepository.save(token);
            saveDelivery(
                    notification,
                    user,
                    token,
                    result.successful()
                            ? NotificationDeliveryStatus.SENT
                            : NotificationDeliveryStatus.FAILED,
                    result.provider(),
                    result.providerMessageId(),
                    result.errorMessage()
            );
        }
    }

    private void saveDelivery(
            Notification notification,
            User user,
            PushDeviceToken token,
            NotificationDeliveryStatus status,
            String provider,
            String providerMessageId,
            String errorMessage
    ) {
        NotificationDelivery delivery = new NotificationDelivery();
        delivery.setNotification(notification);
        delivery.setUser(user);
        delivery.setDeviceToken(token);
        delivery.setChannel(NotificationDeliveryChannel.PUSH);
        delivery.setStatus(status);
        delivery.setProvider(provider);
        delivery.setProviderMessageId(providerMessageId);
        delivery.setErrorMessage(errorMessage);
        if (status == NotificationDeliveryStatus.SENT) {
            delivery.setSentAt(LocalDateTime.now());
        }
        notificationDeliveryRepository.save(delivery);
    }
}
