package com.fleetmatch.email.entity;

import com.fleetmatch.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "email_templates")
public class EmailTemplate extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String templateKey;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, length = 10000)
    private String body;

    @Column(nullable = false)
    private boolean active = true;
}
