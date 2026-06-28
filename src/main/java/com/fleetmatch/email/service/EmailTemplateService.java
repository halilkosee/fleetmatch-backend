package com.fleetmatch.email.service;

import com.fleetmatch.audit.entity.AuditAction;
import com.fleetmatch.audit.service.AuditLogService;
import com.fleetmatch.auth.service.EmailService;
import com.fleetmatch.common.exception.ResourceAlreadyExistsException;
import com.fleetmatch.common.exception.ResourceNotFoundException;
import com.fleetmatch.email.dto.EmailTemplateRequest;
import com.fleetmatch.email.dto.EmailTemplateResponse;
import com.fleetmatch.email.entity.EmailTemplate;
import com.fleetmatch.email.repository.EmailTemplateRepository;
import com.fleetmatch.security.user.CustomUserDetails;
import com.fleetmatch.user.entity.User;
import com.fleetmatch.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailTemplateService {

    private final EmailTemplateRepository emailTemplateRepository;
    private final EmailService emailService;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    @Transactional
    public EmailTemplateResponse createTemplate(
            EmailTemplateRequest request,
            CustomUserDetails currentUser
    ) {
        if (emailTemplateRepository.existsByTemplateKey(request.getTemplateKey())) {
            throw new ResourceAlreadyExistsException("Email template already exists");
        }

        EmailTemplate template = new EmailTemplate();
        apply(template, request);
        EmailTemplate saved = emailTemplateRepository.save(template);
        auditLogService.log(getActor(currentUser), AuditAction.EMAIL_TEMPLATE_CREATED, "EMAIL_TEMPLATE", saved.getId(), "Email template created");
        return toResponse(saved);
    }

    @Transactional
    public EmailTemplateResponse updateTemplate(
            UUID templateId,
            EmailTemplateRequest request,
            CustomUserDetails currentUser
    ) {
        EmailTemplate template = emailTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Email template not found"));

        apply(template, request);
        EmailTemplate saved = emailTemplateRepository.save(template);
        auditLogService.log(getActor(currentUser), AuditAction.EMAIL_TEMPLATE_UPDATED, "EMAIL_TEMPLATE", saved.getId(), "Email template updated");
        return toResponse(saved);
    }

    public List<EmailTemplateResponse> getTemplates() {
        return emailTemplateRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public EmailTemplateResponse getTemplate(UUID templateId) {
        EmailTemplate template = emailTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Email template not found"));
        return toResponse(template);
    }

    @Transactional
    public void sendTemplate(
            String templateKey,
            String email,
            Map<String, String> variables
    ) {
        emailTemplateRepository.findByTemplateKeyAndActiveTrue(templateKey)
                .ifPresent(template -> {
                    String subject = render(template.getSubject(), variables);
                    String body = render(template.getBody(), variables);
                    emailService.sendEmail(email, subject, body);
                    auditLogService.log(null, AuditAction.EMAIL_TEMPLATE_SENT, "EMAIL_TEMPLATE", template.getId(), "Email template sent: " + templateKey);
                });
    }

    private void apply(EmailTemplate template, EmailTemplateRequest request) {
        template.setTemplateKey(request.getTemplateKey());
        template.setSubject(request.getSubject());
        template.setBody(request.getBody());
        template.setActive(request.getActive() == null || request.getActive());
    }

    private String render(String text, Map<String, String> variables) {
        String rendered = text;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue() == null ? "" : entry.getValue());
        }
        return rendered;
    }

    private EmailTemplateResponse toResponse(EmailTemplate template) {
        return new EmailTemplateResponse(
                template.getId(),
                template.getTemplateKey(),
                template.getSubject(),
                template.getBody(),
                template.isActive()
        );
    }

    private User getActor(CustomUserDetails currentUser) {
        if (currentUser == null) {
            return null;
        }
        return userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
