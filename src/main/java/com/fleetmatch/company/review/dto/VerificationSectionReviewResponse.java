package com.fleetmatch.company.review.dto;

import com.fleetmatch.company.review.entity.SectionReviewStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class VerificationSectionReviewResponse {

    private UUID id;
    private String sectionKey;
    private String label;
    private SectionReviewStatus status;
    private String notes;
    private UUID reviewerUserId;
    private LocalDateTime reviewedAt;
}
