package com.fleetmatch.company.review.dto;

import com.fleetmatch.company.review.entity.VerificationRiskLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class VerificationRiskResponse {

    private UUID id;
    private int score;
    private VerificationRiskLevel level;
    private List<String> signals;
}
