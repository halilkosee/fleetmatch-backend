package com.fleetmatch.support.dto;

import com.fleetmatch.support.category.SupportTicketCategory;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class SupportReplyTemplateResponse {

    private UUID id;
    private String templateKey;
    private String title;
    private SupportTicketCategory category;
    private String body;
    private boolean active;
}
