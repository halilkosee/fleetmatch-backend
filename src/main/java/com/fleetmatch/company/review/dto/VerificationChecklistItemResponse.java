package com.fleetmatch.company.review.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class VerificationChecklistItemResponse {

    private UUID id;
    private String itemKey;
    private String label;
    private boolean mandatory;
    private boolean completed;
    private String notes;
    private UUID reviewerUserId;
    private LocalDateTime completedAt;
}
