package com.fleetmatch.email.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class EmailTemplateResponse {

    private UUID id;
    private String templateKey;
    private String subject;
    private String body;
    private boolean active;
}
