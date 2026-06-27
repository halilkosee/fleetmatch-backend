package com.fleetmatch.audit.service;

import com.fleetmatch.audit.dto.AuditLogResponse;
import com.fleetmatch.audit.entity.AuditAction;
import com.fleetmatch.audit.entity.AuditLog;
import com.fleetmatch.audit.repository.AuditLogRepository;
import com.fleetmatch.user.entity.User;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void log(
            User actor,
            AuditAction action,
            String entityType,
            UUID entityId,
            String details
    ) {
        AuditLog auditLog = new AuditLog();
        auditLog.setActorUserId(actor == null ? null : actor.getId());
        auditLog.setActorEmail(actor == null ? null : actor.getEmail());
        auditLog.setActorCompanyId(actor == null || actor.getCompany() == null
                ? null
                : actor.getCompany().getId());
        auditLog.setAction(action);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setDetails(details);
        auditLog.setIpAddress(resolveIpAddress());
        auditLogRepository.save(auditLog);
    }

    public Page<AuditLogResponse> search(
            AuditAction action,
            String entityType,
            UUID entityId,
            String actorEmail,
            UUID actorCompanyId,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    ) {
        Specification<AuditLog> specification = (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();

            if (action != null) {
                predicates.add(cb.equal(root.get("action"), action));
            }
            if (entityType != null && !entityType.isBlank()) {
                predicates.add(cb.equal(root.get("entityType"), entityType));
            }
            if (entityId != null) {
                predicates.add(cb.equal(root.get("entityId"), entityId));
            }
            if (actorEmail != null && !actorEmail.isBlank()) {
                predicates.add(cb.equal(root.get("actorEmail"), actorEmail));
            }
            if (actorCompanyId != null) {
                predicates.add(cb.equal(root.get("actorCompanyId"), actorCompanyId));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };

        return auditLogRepository.findAll(specification, pageable)
                .map(this::toResponse);
    }

    private AuditLogResponse toResponse(AuditLog auditLog) {
        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .actorUserId(auditLog.getActorUserId())
                .actorEmail(auditLog.getActorEmail())
                .actorCompanyId(auditLog.getActorCompanyId())
                .action(auditLog.getAction())
                .entityType(auditLog.getEntityType())
                .entityId(auditLog.getEntityId())
                .details(auditLog.getDetails())
                .ipAddress(auditLog.getIpAddress())
                .createdAt(auditLog.getCreatedAt())
                .build();
    }

    private String resolveIpAddress() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }

        String forwardedFor = attributes.getRequest().getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return attributes.getRequest().getRemoteAddr();
    }
}
