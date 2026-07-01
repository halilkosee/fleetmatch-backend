package com.fleetmatch.notification.service;

import com.fleetmatch.company.entity.Company;
import com.fleetmatch.notification.entity.DevicePlatform;
import com.fleetmatch.notification.entity.Notification;
import com.fleetmatch.notification.entity.NotificationDelivery;
import com.fleetmatch.notification.entity.NotificationDeliveryStatus;
import com.fleetmatch.notification.entity.NotificationType;
import com.fleetmatch.notification.entity.PushDeviceToken;
import com.fleetmatch.notification.repository.NotificationDeliveryRepository;
import com.fleetmatch.notification.repository.PushDeviceTokenRepository;
import com.fleetmatch.user.entity.User;
import com.fleetmatch.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PushNotificationDispatcherTest {

    @Mock
    private PushDeviceTokenRepository pushDeviceTokenRepository;
    @Mock
    private NotificationDeliveryRepository notificationDeliveryRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PushNotificationProvider pushNotificationProvider;

    @InjectMocks
    private PushNotificationDispatcher dispatcher;

    @Test
    void dispatchToUserSendsPushAndStoresDelivery() {
        User user = user();
        Notification notification = notification(user);
        PushDeviceToken token = token(user);
        when(pushDeviceTokenRepository.findByUserIdAndActiveTrue(user.getId())).thenReturn(List.of(token));
        when(pushNotificationProvider.send(any(PushMessage.class)))
                .thenReturn(PushSendResult.sent("fcm", "projects/demo/messages/1"));

        dispatcher.dispatchToUser(notification, user);

        ArgumentCaptor<NotificationDelivery> captor =
                ArgumentCaptor.forClass(NotificationDelivery.class);
        verify(notificationDeliveryRepository).save(captor.capture());
        assertEquals(NotificationDeliveryStatus.SENT, captor.getValue().getStatus());
        assertEquals("fcm", captor.getValue().getProvider());
        assertEquals("projects/demo/messages/1", captor.getValue().getProviderMessageId());
        verify(pushDeviceTokenRepository).save(token);
    }

    @Test
    void dispatchToUserStoresSkippedDeliveryWhenNoActiveDeviceExists() {
        User user = user();
        Notification notification = notification(user);
        when(pushDeviceTokenRepository.findByUserIdAndActiveTrue(user.getId())).thenReturn(List.of());

        dispatcher.dispatchToUser(notification, user);

        ArgumentCaptor<NotificationDelivery> captor =
                ArgumentCaptor.forClass(NotificationDelivery.class);
        verify(notificationDeliveryRepository).save(captor.capture());
        assertEquals(NotificationDeliveryStatus.SKIPPED, captor.getValue().getStatus());
    }

    @Test
    void dispatchToCompanySendsToCompanyUsers() {
        User user = user();
        Company company = new Company();
        company.setId(UUID.randomUUID());
        user.setCompany(company);
        Notification notification = notification(user);
        notification.setUser(null);
        notification.setCompany(company);
        PushDeviceToken token = token(user);
        when(userRepository.findByCompanyId(company.getId())).thenReturn(List.of(user));
        when(pushDeviceTokenRepository.findByUserIdInAndActiveTrue(List.of(user.getId())))
                .thenReturn(List.of(token));
        when(pushNotificationProvider.send(any(PushMessage.class)))
                .thenReturn(PushSendResult.sent("fcm", "message-id"));

        dispatcher.dispatchToCompany(notification);

        verify(pushNotificationProvider).send(any(PushMessage.class));
        verify(notificationDeliveryRepository).save(any(NotificationDelivery.class));
    }

    private Notification notification(User user) {
        Notification notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setUser(user);
        notification.setType(NotificationType.NEW_MESSAGE);
        notification.setTitle("New message");
        notification.setMessage("You have a new message");
        notification.setRelatedEntityType("CONVERSATION");
        notification.setRelatedEntityId(UUID.randomUUID());
        return notification;
    }

    private PushDeviceToken token(User user) {
        PushDeviceToken token = new PushDeviceToken();
        token.setId(UUID.randomUUID());
        token.setUser(user);
        token.setToken("fcm-token");
        token.setPlatform(DevicePlatform.IOS);
        token.setActive(true);
        return token;
    }

    private User user() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setFirstName("Mobile");
        user.setLastName("User");
        user.setEmail("mobile@example.test");
        user.setPassword("encoded");
        return user;
    }
}
