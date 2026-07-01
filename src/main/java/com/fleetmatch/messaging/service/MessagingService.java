package com.fleetmatch.messaging.service;

import com.fleetmatch.audit.entity.AuditAction;
import com.fleetmatch.audit.service.AuditLogService;
import com.fleetmatch.common.exception.BusinessRuleException;
import com.fleetmatch.common.exception.ResourceNotFoundException;
import com.fleetmatch.company.entity.Company;
import com.fleetmatch.messaging.dto.ConversationResponse;
import com.fleetmatch.messaging.dto.ConversationStreamEvent;
import com.fleetmatch.messaging.dto.CreateMessageRequest;
import com.fleetmatch.messaging.dto.MessageResponse;
import com.fleetmatch.messaging.entity.Conversation;
import com.fleetmatch.messaging.entity.Message;
import com.fleetmatch.messaging.repository.ConversationRepository;
import com.fleetmatch.messaging.repository.MessageRepository;
import com.fleetmatch.notification.event.NotificationType;
import com.fleetmatch.notification.inapp.service.NotificationService;
import com.fleetmatch.offer.entity.Offer;
import com.fleetmatch.security.user.CustomUserDetails;
import com.fleetmatch.user.entity.CompanyUserRole;
import com.fleetmatch.user.entity.PlatformRole;
import com.fleetmatch.user.entity.User;
import com.fleetmatch.user.repository.UserRepository;
import com.fleetmatch.user.service.UserVerificationGuard;
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
public class MessagingService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final UserVerificationGuard userVerificationGuard;
    private final NotificationService notificationService;
    private final ConversationRealtimeService conversationRealtimeService;
    private final AuditLogService auditLogService;

    @Transactional
    public Conversation createConversationForSelectedOffer(Offer offer) {
        return conversationRepository.findByLoadId(
                        offer.getLoad().getId()
                )
                .map(conversation -> {
                    conversation.setFleetCompany(
                            offer.getFleetUser().getCompany()
                    );
                    conversation.setArchivedAt(null);
                    return conversationRepository.save(conversation);
                })
                .orElseGet(() -> {
                    Conversation conversation = new Conversation();
                    conversation.setLoad(offer.getLoad());
                    conversation.setBrokerCompany(
                            offer.getLoad().getBrokerCompany()
                    );
                    conversation.setFleetCompany(
                            offer.getFleetUser().getCompany()
                    );

                    return conversationRepository.save(conversation);
                });
    }

    @Transactional
    public void archiveConversation(UUID loadId) {
        conversationRepository.findByLoadId(loadId)
                .ifPresent(conversation -> {
                    conversation.setArchivedAt(LocalDateTime.now());

                    messageRepository.findByConversationId(conversation.getId())
                            .stream()
                            .filter(message -> !message.isDeleted())
                            .forEach(message -> message.setDeletedAt(
                                    LocalDateTime.now()
                            ));

                    conversationRepository.save(conversation);
                });
    }

    public Page<ConversationResponse> getConversations(
            Pageable pageable,
            CustomUserDetails currentUser
    ) {
        User user = getCurrentUser(currentUser);
        Company company = requireMessagingCompany(user);

        return conversationRepository.findByParticipantCompanyId(
                company.getId(),
                pageable
        ).map(conversation -> toConversationResponse(
                conversation,
                company.getId()
        ));
    }

    public ConversationResponse getConversationByLoad(
            UUID loadId,
            CustomUserDetails currentUser
    ) {
        User user = getCurrentUser(currentUser);
        Conversation conversation = conversationRepository.findByLoadId(loadId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Conversation not found"
                ));

        requireConversationParticipant(
                conversation,
                user
        );

        return toConversationResponse(
                conversation,
                user.getCompany().getId()
        );
    }

    public Page<MessageResponse> getMessages(
            UUID conversationId,
            Pageable pageable,
            CustomUserDetails currentUser
    ) {
        User user = getCurrentUser(currentUser);
        Conversation conversation = getConversationForParticipant(
                conversationId,
                user
        );

        return messageRepository.findByConversationIdAndDeletedAtIsNullOrderByCreatedAtAsc(
                conversation.getId(),
                pageable
        ).map(this::toMessageResponse);
    }

    public void validateConversationAccess(
            UUID conversationId,
            CustomUserDetails currentUser
    ) {
        User user = getCurrentUser(currentUser);
        getConversationForParticipant(conversationId, user);
    }

    @Transactional
    public MessageResponse sendMessage(
            UUID conversationId,
            CreateMessageRequest request,
            CustomUserDetails currentUser
    ) {
        User user = getCurrentUser(currentUser);
        Conversation conversation = getConversationForParticipant(
                conversationId,
                user
        );

        if (conversation.isArchived()) {
            throw new BusinessRuleException(
                    "Archived conversations cannot receive messages"
            );
        }

        Message message = new Message();
        message.setConversation(conversation);
        message.setSenderUser(user);
        message.setSenderCompany(user.getCompany());
        message.setBody(request.getBody());

        MessageResponse response = toMessageResponse(
                messageRepository.save(message)
        );

        Company recipientCompany = conversation.getBrokerCompany().getId().equals(
                user.getCompany().getId()
        )
                ? conversation.getFleetCompany()
                : conversation.getBrokerCompany();

        notificationService.createForCompany(
                recipientCompany,
                NotificationType.NEW_MESSAGE,
                "New message",
                "A new message was sent in a load conversation",
                "CONVERSATION",
                conversation.getId()
        );
        auditLogService.log(
                user,
                AuditAction.MESSAGE_SENT,
                "MESSAGE",
                response.getId(),
                "Message sent in conversation " + conversation.getId()
        );
        publishConversationEvent("MESSAGE_CREATED", response);

        return response;
    }

    @Transactional
    public MessageResponse markMessageAsRead(
            UUID conversationId,
            UUID messageId,
            CustomUserDetails currentUser
    ) {
        User user = getCurrentUser(currentUser);
        Conversation conversation = getConversationForParticipant(
                conversationId,
                user
        );

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Message not found"
                ));

        if (!message.getConversation().getId().equals(
                conversation.getId()
        )) {
            throw new AccessDeniedException(
                    "Message does not belong to this conversation"
            );
        }

        if (message.isDeleted()) {
            throw new BusinessRuleException(
                    "Deleted messages cannot be marked as read"
            );
        }

        if (message.getSenderUser().getId().equals(user.getId())) {
            throw new BusinessRuleException(
                    "You cannot mark your own message as read"
            );
        }

        if (!message.isRead()) {
            message.setReadAt(LocalDateTime.now());
        }

        MessageResponse response = toMessageResponse(
                messageRepository.save(message)
        );
        auditLogService.log(
                user,
                AuditAction.MESSAGE_READ,
                "MESSAGE",
                message.getId(),
                "Message marked as read"
        );
        publishConversationEvent("MESSAGE_READ", response);
        return response;
    }

    @Transactional
    public MessageResponse deleteMessage(
            UUID conversationId,
            UUID messageId,
            CustomUserDetails currentUser
    ) {
        User user = getCurrentUser(currentUser);
        Conversation conversation = getConversationForParticipant(
                conversationId,
                user
        );

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Message not found"
                ));

        if (!message.getConversation().getId().equals(
                conversation.getId()
        )) {
            throw new AccessDeniedException(
                    "Message does not belong to this conversation"
            );
        }

        if (!message.getSenderUser().getId().equals(user.getId()) &&
                user.getPlatformRole() != PlatformRole.ADMIN) {
            throw new AccessDeniedException(
                    "You can only delete your own messages"
            );
        }

        if (message.isDeleted()) {
            throw new BusinessRuleException(
                    "Message is already deleted"
            );
        }

        message.setDeletedAt(LocalDateTime.now());

        MessageResponse response = toMessageResponse(
                messageRepository.save(message)
        );
        auditLogService.log(
                user,
                AuditAction.MESSAGE_DELETED,
                "MESSAGE",
                message.getId(),
                "Message soft deleted"
        );
        publishConversationEvent("MESSAGE_DELETED", response);
        return response;
    }

    private Conversation getConversationForParticipant(
            UUID conversationId,
            User user
    ) {
        Conversation conversation = conversationRepository
                .findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Conversation not found"
                ));

        requireConversationParticipant(
                conversation,
                user
        );

        return conversation;
    }

    private User getCurrentUser(CustomUserDetails currentUser) {
        return userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found"
                ));
    }

    private Company requireMessagingCompany(User user) {
        if (user.getCompany() == null) {
            throw new AccessDeniedException(
                    "Only company users can access conversations"
            );
        }

        if (user.getCompanyUserRole() == CompanyUserRole.DRIVER) {
            throw new AccessDeniedException(
                    "Drivers cannot use messaging"
            );
        }

        userVerificationGuard.requireVerifiedForCoreWorkflow(user);

        return user.getCompany();
    }

    private void requireConversationParticipant(
            Conversation conversation,
            User user
    ) {
        Company company = requireMessagingCompany(user);

        boolean participant =
                conversation.getBrokerCompany().getId().equals(
                        company.getId()
                ) ||
                        conversation.getFleetCompany().getId().equals(
                                company.getId()
                        );

        if (!participant) {
            throw new AccessDeniedException(
                    "You can only access conversations for your company"
            );
        }
    }

    private ConversationResponse toConversationResponse(
            Conversation conversation,
            UUID viewerCompanyId
    ) {
        Message lastMessage = messageRepository
                .findTopByConversationIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                        conversation.getId()
                )
                .orElse(null);

        return new ConversationResponse(
                conversation.getId(),
                conversation.getLoad().getId(),
                conversation.getBrokerCompany().getId(),
                conversation.getBrokerCompany().getLegalName(),
                conversation.getFleetCompany().getId(),
                conversation.getFleetCompany().getLegalName(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt(),
                lastMessage == null ? null : lastMessage.getBody(),
                lastMessage == null ? null : lastMessage.getCreatedAt(),
                messageRepository.countUnreadForCompany(
                        conversation.getId(),
                        viewerCompanyId
                )
        );
    }

    private MessageResponse toMessageResponse(Message message) {
        User sender = message.getSenderUser();
        Company senderCompany = message.getSenderCompany();

        return new MessageResponse(
                message.getId(),
                message.getConversation().getId(),
                sender.getId(),
                sender.getFirstName() + " " + sender.getLastName(),
                senderCompany.getId(),
                senderCompany.getLegalName(),
                message.isDeleted() ? null : message.getBody(),
                message.isRead(),
                message.getReadAt(),
                message.isDeleted(),
                message.getDeletedAt(),
                message.getCreatedAt(),
                message.getUpdatedAt()
        );
    }

    private void publishConversationEvent(
            String type,
            MessageResponse response
    ) {
        conversationRealtimeService.publish(
                response.getConversationId(),
                new ConversationStreamEvent(
                        type,
                        response.getConversationId(),
                        LocalDateTime.now(),
                        response
                )
        );
    }
}
