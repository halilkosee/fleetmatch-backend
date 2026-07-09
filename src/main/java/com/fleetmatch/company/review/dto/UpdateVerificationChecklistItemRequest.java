package com.fleetmatch.company.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateVerificationChecklistItemRequest {

    @NotBlank
    private String itemKey;

    private boolean completed;

    @Size(max = 1000)
    private String notes;
}
