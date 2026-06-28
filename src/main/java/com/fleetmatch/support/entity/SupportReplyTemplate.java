package com.fleetmatch.support.entity;

import com.fleetmatch.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "support_reply_templates")
public class SupportReplyTemplate extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String templateKey;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SupportTicketCategory category;

    @Column(nullable = false, length = 4000)
    private String body;

    @Column(nullable = false)
    private boolean active = true;
}
