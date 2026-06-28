package com.fleetmatch.support.dto;

import com.fleetmatch.support.entity.SupportTicketCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SupportReplyTemplateRequest {

    @NotBlank
    @Size(max = 100)
    private String templateKey;

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotNull
    private SupportTicketCategory category;

    @NotBlank
    @Size(max = 4000)
    private String body;

    private boolean active = true;
}
