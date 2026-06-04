package com.fleetmatch.company.entity;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import com.fleetmatch.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "companies")
public class Company extends BaseEntity {

    @Column(nullable = false)
    private String legalName;

    private String dbaName;

    @Column(nullable = false)
    private String email;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompanyType type;

    @Column(nullable = false)
    private Boolean verified = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompanyVerificationStatus verificationStatus =
            CompanyVerificationStatus.PENDING;

    @Column(length = 100)
    private String mcNumber;

    @Column(length = 100)
    private String dotNumber;

    @Column(length = 255)
    private String website;
}