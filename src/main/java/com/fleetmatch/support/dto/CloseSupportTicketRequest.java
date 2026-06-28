package com.fleetmatch.support.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CloseSupportTicketRequest {

    @Size(max = 1000)
    private String resolutionSummary;

    @Min(1)
    @Max(5)
    private Integer satisfactionRating;

    @Size(max = 1000)
    private String satisfactionComment;
}
