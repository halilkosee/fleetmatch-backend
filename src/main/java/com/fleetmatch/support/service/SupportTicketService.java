package com.fleetmatch.support.service;

import com.fleetmatch.common.exception.ResourceNotFoundException;
import com.fleetmatch.security.user.CustomUserDetails;
import com.fleetmatch.support.dto.CloseSupportTicketRequest;
import com.fleetmatch.support.dto.CreateSupportTicketRequest;
import com.fleetmatch.support.dto.SendSupportMessageRequest;
import com.fleetmatch.support.dto.SupportMessageResponse;
import com.fleetmatch.support.dto.SupportReplyTemplateRequest;
import com.fleetmatch.support.dto.SupportReplyTemplateResponse;
import com.fleetmatch.support.dto.SupportTicketResponse;
import com.fleetmatch.support.entity.SupportMessage;
import com.fleetmatch.support.entity.SupportMessageSenderType;
import com.fleetmatch.support.entity.SupportReplyTemplate;
import com.fleetmatch.support.entity.SupportTicket;
import com.fleetmatch.support.entity.SupportTicketPriority;
import com.fleetmatch.support.entity.SupportTicketStatus;
import com.fleetmatch.support.repository.SupportMessageRepository;
import com.fleetmatch.support.repository.SupportReplyTemplateRepository;
import com.fleetmatch.support.repository.SupportTicketRepository;
import com.fleetmatch.user.entity.PlatformRole;
import com.fleetmatch.user.entity.User;
import com.fleetmatch.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import com.fleetmatch.common.exception.BusinessRuleException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SupportTicketService {

    private final SupportTicketRepository supportTicketRepository;
    private final SupportMessageRepository supportMessageRepository;
    private final SupportReplyTemplateRepository supportReplyTemplateRepository;
    private final UserRepository userRepository;

    @Transactional
    public SupportTicketResponse createTicket(
            CreateSupportTicketRequest request,
            CustomUserDetails currentUser
    ) {
        User user = getUser(currentUser);

        SupportTicket ticket = new SupportTicket();
        ticket.setUser(user);
        ticket.setCompany(user.getCompany());
        ticket.setCategory(request.getCategory());
        ticket.setPriority(request.getPriority());
        ticket.setSubject(request.getSubject());
        ticket.setMessage(request.getMessage());
        ticket.setStatus(SupportTicketStatus.WAITING_ADMIN);
        ticket.setExpectedResponseAt(expectedResponseAt(request.getPriority()));

        SupportTicket saved = supportTicketRepository.save(ticket);
        saveMessage(saved, user, SupportMessageSenderType.USER, request.getMessage());

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<SupportTicketResponse> getMyTickets(
            CustomUserDetails currentUser
    ) {
        User user = getUser(currentUser);

        return supportTicketRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SupportMessageResponse sendUserMessage(
            UUID ticketId,
            SendSupportMessageRequest request,
            CustomUserDetails currentUser
    ) {
        User user = getUser(currentUser);
        SupportTicket ticket = getTicket(ticketId);
        ensureTicketOwner(ticket, user);

        ticket.setStatus(SupportTicketStatus.WAITING_ADMIN);
        supportTicketRepository.save(ticket);

        return toMessageResponse(
                saveMessage(ticket, user, SupportMessageSenderType.USER, request.getMessage())
        );
    }

    @Transactional(readOnly = true)
    public List<SupportMessageResponse> getUserMessages(
            UUID ticketId,
            CustomUserDetails currentUser
    ) {
        User user = getUser(currentUser);
        SupportTicket ticket = getTicket(ticketId);
        ensureTicketOwner(ticket, user);

        return getMessages(ticketId);
    }

    @Transactional(readOnly = true)
    public List<SupportTicketResponse> getAdminTickets(
            SupportTicketStatus status
    ) {
        List<SupportTicket> tickets = status == null
                ? supportTicketRepository.findAll()
                : supportTicketRepository.findByStatusOrderByCreatedAtAsc(status);

        return tickets.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SupportTicketResponse getAdminTicket(UUID ticketId) {
        return toResponse(getTicket(ticketId));
    }

    @Transactional(readOnly = true)
    public List<SupportMessageResponse> getAdminMessages(UUID ticketId) {
        getTicket(ticketId);
        return getMessages(ticketId);
    }

    @Transactional
    public SupportMessageResponse sendAdminMessage(
            UUID ticketId,
            SendSupportMessageRequest request,
            CustomUserDetails currentUser
    ) {
        User admin = getUser(currentUser);
        if (admin.getPlatformRole() != PlatformRole.ADMIN) {
            throw new AccessDeniedException("Only admins can reply to support tickets");
        }

        SupportTicket ticket = getTicket(ticketId);
        ticket.setStatus(SupportTicketStatus.ANSWERED);
        ticket.setAdminReply(request.getMessage());
        ticket.setAnsweredAt(LocalDateTime.now());
        supportTicketRepository.save(ticket);

        return toMessageResponse(
                saveMessage(ticket, admin, SupportMessageSenderType.ADMIN, request.getMessage())
        );
    }

    @Transactional
    public SupportTicketResponse closeUserTicket(
            UUID ticketId,
            CloseSupportTicketRequest request,
            CustomUserDetails currentUser
    ) {
        User user = getUser(currentUser);
        SupportTicket ticket = getTicket(ticketId);
        ensureTicketOwner(ticket, user);

        closeTicketFields(ticket, request);
        return toResponse(supportTicketRepository.save(ticket));
    }

    @Transactional
    public SupportTicketResponse closeTicket(
            UUID ticketId,
            CloseSupportTicketRequest request,
            CustomUserDetails currentUser
    ) {
        User admin = getUser(currentUser);
        if (admin.getPlatformRole() != PlatformRole.ADMIN) {
            throw new AccessDeniedException("Only admins can close support tickets");
        }

        SupportTicket ticket = getTicket(ticketId);
        closeTicketFields(ticket, request);
        return toResponse(supportTicketRepository.save(ticket));
    }

    @Transactional(readOnly = true)
    public List<SupportReplyTemplateResponse> getTemplates() {
        return supportReplyTemplateRepository
                .findByActiveTrueOrderByTitleAsc()
                .stream()
                .map(this::toTemplateResponse)
                .toList();
    }

    @Transactional
    public SupportReplyTemplateResponse createTemplate(
            SupportReplyTemplateRequest request
    ) {
        if (supportReplyTemplateRepository.existsByTemplateKey(request.getTemplateKey())) {
            throw new BusinessRuleException("Support reply template already exists");
        }

        SupportReplyTemplate template = new SupportReplyTemplate();
        applyTemplate(template, request);
        return toTemplateResponse(supportReplyTemplateRepository.save(template));
    }

    @Transactional
    public SupportReplyTemplateResponse updateTemplate(
            UUID templateId,
            SupportReplyTemplateRequest request
    ) {
        SupportReplyTemplate template =
                supportReplyTemplateRepository.findById(templateId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Support reply template not found"
                                ));

        supportReplyTemplateRepository.findByTemplateKey(request.getTemplateKey())
                .filter(existing -> !existing.getId().equals(templateId))
                .ifPresent(existing -> {
                    throw new BusinessRuleException("Support reply template already exists");
                });

        applyTemplate(template, request);
        return toTemplateResponse(supportReplyTemplateRepository.save(template));
    }

    private LocalDateTime expectedResponseAt(SupportTicketPriority priority) {
        LocalDateTime now = LocalDateTime.now();
        if (priority == SupportTicketPriority.URGENT) {
            return now.plusHours(4);
        }
        return now.plusDays(1);
    }

    private User getUser(CustomUserDetails currentUser) {
        return userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private SupportTicket getTicket(UUID ticketId) {
        return supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Support ticket not found"));
    }

    private void ensureTicketOwner(SupportTicket ticket, User user) {
        if (!ticket.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You cannot access this support ticket");
        }
    }

    private void closeTicketFields(
            SupportTicket ticket,
            CloseSupportTicketRequest request
    ) {
        ticket.setStatus(SupportTicketStatus.CLOSED);
        ticket.setClosedAt(LocalDateTime.now());
        if (request != null) {
            ticket.setResolutionSummary(request.getResolutionSummary());
            ticket.setSatisfactionRating(request.getSatisfactionRating());
            ticket.setSatisfactionComment(request.getSatisfactionComment());
        }
    }

    private SupportMessage saveMessage(
            SupportTicket ticket,
            User sender,
            SupportMessageSenderType senderType,
            String message
    ) {
        SupportMessage supportMessage = new SupportMessage();
        supportMessage.setTicket(ticket);
        supportMessage.setSenderUser(sender);
        supportMessage.setSenderType(senderType);
        supportMessage.setMessage(message);
        return supportMessageRepository.save(supportMessage);
    }

    private List<SupportMessageResponse> getMessages(UUID ticketId) {
        return supportMessageRepository
                .findByTicketIdOrderByCreatedAtAsc(ticketId)
                .stream()
                .map(this::toMessageResponse)
                .toList();
    }

    private void applyTemplate(
            SupportReplyTemplate template,
            SupportReplyTemplateRequest request
    ) {
        template.setTemplateKey(request.getTemplateKey());
        template.setTitle(request.getTitle());
        template.setCategory(request.getCategory());
        template.setBody(request.getBody());
        template.setActive(request.isActive());
    }

    private SupportTicketResponse toResponse(SupportTicket ticket) {
        User user = ticket.getUser();
        var company = ticket.getCompany();

        return new SupportTicketResponse(
                ticket.getId(),
                user.getId(),
                user.getFirstName() + " " + user.getLastName(),
                user.getEmail(),
                company == null ? null : company.getId(),
                company == null ? null : company.getLegalName(),
                company == null ? null : company.getType(),
                ticket.getCategory(),
                ticket.getPriority(),
                ticket.getSubject(),
                ticket.getMessage(),
                ticket.getStatus(),
                ticket.getAdminReply(),
                ticket.getCreatedAt(),
                ticket.getExpectedResponseAt(),
                ticket.getAnsweredAt(),
                ticket.getClosedAt(),
                ticket.getResolutionSummary(),
                ticket.getSatisfactionRating(),
                ticket.getSatisfactionComment()
        );
    }

    private SupportMessageResponse toMessageResponse(SupportMessage message) {
        User sender = message.getSenderUser();
        return new SupportMessageResponse(
                message.getId(),
                message.getTicket().getId(),
                sender.getId(),
                sender.getFirstName() + " " + sender.getLastName(),
                message.getSenderType(),
                message.getMessage(),
                message.getCreatedAt()
        );
    }

    private SupportReplyTemplateResponse toTemplateResponse(
            SupportReplyTemplate template
    ) {
        return new SupportReplyTemplateResponse(
                template.getId(),
                template.getTemplateKey(),
                template.getTitle(),
                template.getCategory(),
                template.getBody(),
                template.isActive()
        );
    }
}
