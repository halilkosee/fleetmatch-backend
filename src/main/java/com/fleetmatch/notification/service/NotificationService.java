package com.fleetmatch.notification.service;

import com.fleetmatch.common.exception.ResourceNotFoundException;
import com.fleetmatch.company.entity.Company;
import com.fleetmatch.notification.dto.NotificationResponse;
import com.fleetmatch.notification.dto.UnreadCountResponse;
import com.fleetmatch.notification.entity.Notification;
import com.fleetmatch.notification.entity.NotificationType;
import com.fleetmatch.notification.repository.NotificationRepository;
import com.fleetmatch.security.user.CustomUserDetails;
import com.fleetmatch.user.entity.User;
import com.fleetmatch.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Transactional
    public void createForCompany(
            Company company,
            NotificationType type,
            String title,
            String message,
            String relatedEntityType,
            UUID relatedEntityId
    ) {
        Notification notification = new Notification();
        notification.setCompany(company);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRelatedEntityType(relatedEntityType);
        notification.setRelatedEntityId(relatedEntityId);
        notificationRepository.save(notification);
    }

    @Transactional
    public void createForUser(
            User user,
            NotificationType type,
            String title,
            String message,
            String relatedEntityType,
            UUID relatedEntityId
    ) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRelatedEntityType(relatedEntityType);
        notification.setRelatedEntityId(relatedEntityId);
        notificationRepository.save(notification);
    }

    public Page<NotificationResponse> getNotifications(
            CustomUserDetails currentUser,
            Pageable pageable
    ) {
        User user = getCurrentUser(currentUser);
        UUID companyId = user.getCompany() == null ? null : user.getCompany().getId();

        return notificationRepository
                .findByUserIdOrCompanyIdOrderByCreatedAtDesc(
                        user.getId(),
                        companyId,
                        pageable
                )
                .map(this::toResponse);
    }

    public UnreadCountResponse getUnreadCount(CustomUserDetails currentUser) {
        User user = getCurrentUser(currentUser);
        UUID companyId = user.getCompany() == null ? null : user.getCompany().getId();

        return new UnreadCountResponse(
                notificationRepository.countUnreadForUserOrCompany(
                        user.getId(),
                        companyId
                )
        );
    }

    @Transactional
    public NotificationResponse markRead(
            UUID notificationId,
            CustomUserDetails currentUser
    ) {
        User user = getCurrentUser(currentUser);
        Notification notification = getAccessibleNotification(notificationId, user);

        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
        }

        return toResponse(notificationRepository.save(notification));
    }

    @Transactional
    public void markAllRead(CustomUserDetails currentUser) {
        User user = getCurrentUser(currentUser);
        UUID companyId = user.getCompany() == null ? null : user.getCompany().getId();

        notificationRepository
                .findByUserIdOrCompanyIdOrderByCreatedAtDesc(
                        user.getId(),
                        companyId,
                        Pageable.unpaged()
                )
                .forEach(notification -> {
                    if (notification.getReadAt() == null) {
                        notification.setReadAt(LocalDateTime.now());
                    }
                });
    }

    private Notification getAccessibleNotification(UUID notificationId, User user) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        boolean userMatch = notification.getUser() != null &&
                notification.getUser().getId().equals(user.getId());
        boolean companyMatch = notification.getCompany() != null &&
                user.getCompany() != null &&
                notification.getCompany().getId().equals(user.getCompany().getId());

        if (!userMatch && !companyMatch) {
            throw new AccessDeniedException("You cannot access this notification");
        }

        return notification;
    }

    private User getCurrentUser(CustomUserDetails currentUser) {
        return userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .readAt(notification.getReadAt())
                .relatedEntityType(notification.getRelatedEntityType())
                .relatedEntityId(notification.getRelatedEntityId())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
