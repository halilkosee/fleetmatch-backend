package com.fleetmatch.email.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailTemplateRequest {

    @NotBlank
    @Size(max = 100)
    private String templateKey;

    @NotBlank
    @Size(max = 255)
    private String subject;

    @NotBlank
    @Size(max = 10000)
    private String body;

    private Boolean active;
}
