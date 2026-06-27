package com.fleetmatch.audit.dto;

import com.fleetmatch.audit.entity.AuditAction;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class AuditLogResponse {

    private UUID id;
    private UUID actorUserId;
    private String actorEmail;
    private UUID actorCompanyId;
    private AuditAction action;
    private String entityType;
    private UUID entityId;
    private String details;
    private String ipAddress;
    private LocalDateTime createdAt;
}
