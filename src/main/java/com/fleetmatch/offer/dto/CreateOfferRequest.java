package com.fleetmatch.offer.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateOfferRequest {

    @NotNull
    @Positive
    private BigDecimal amount;

    private String message;
}