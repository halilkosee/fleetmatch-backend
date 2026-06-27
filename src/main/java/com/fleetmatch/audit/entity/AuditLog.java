package com.fleetmatch.audit.entity;

import com.fleetmatch.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "audit_logs")
public class AuditLog extends BaseEntity {

    private UUID actorUserId;

    private String actorEmail;

    private UUID actorCompanyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 80)
    private AuditAction action;

    @Column(nullable = false, length = 80)
    private String entityType;

    private UUID entityId;

    @Column(length = 2000)
    private String details;

    private String ipAddress;
}
