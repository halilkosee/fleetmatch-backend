package com.fleetmatch.company.review.dto;

import com.fleetmatch.company.review.entity.SectionReviewStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateVerificationSectionReviewRequest {

    @NotBlank
    private String sectionKey;

    @NotNull
    private SectionReviewStatus status;

    @Size(max = 1000)
    private String notes;
}
