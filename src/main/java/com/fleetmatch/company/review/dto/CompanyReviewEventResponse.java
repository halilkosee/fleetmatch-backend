package com.fleetmatch.company.review.dto;

import com.fleetmatch.company.review.entity.CompanyReviewAction;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class CompanyReviewEventResponse {

    private UUID id;
    private CompanyReviewAction action;
    private UUID actorUserId;
    private String actorName;
    private UUID relatedDocumentId;
    private String reason;
    private String notes;
    private LocalDateTime createdAt;
}
