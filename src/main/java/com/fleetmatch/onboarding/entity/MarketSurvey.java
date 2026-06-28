package com.fleetmatch.onboarding.entity;

import com.fleetmatch.common.entity.BaseEntity;
import com.fleetmatch.company.entity.Company;
import com.fleetmatch.company.entity.CompanyType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "market_surveys")
public class MarketSurvey extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false, unique = true)
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompanyType companyType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> operatingStates;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> equipmentTypes;

    private Integer averageLoadsPerWeek;
    private Integer fleetSize;
    private String currentLoadBoard;
    private String currentTms;
    private Boolean futureIntegrationInterest;

    @Column(length = 4000)
    private String biggestOperationalChallenges;

    private String homeState;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> preferredRegions;

    private Integer preferredMileage;
    private Boolean dedicatedRouteInterest;
}
