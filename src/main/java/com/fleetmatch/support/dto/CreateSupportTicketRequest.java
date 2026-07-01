package com.fleetmatch.support.dto;

import com.fleetmatch.support.category.SupportTicketCategory;
import com.fleetmatch.support.category.SupportTicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateSupportTicketRequest {

    @NotNull
    private SupportTicketCategory category;

    @NotNull
    private SupportTicketPriority priority;

    @NotBlank
    @Size(max = 200)
    private String subject;

    @NotBlank
    @Size(max = 4000)
    private String message;
}
