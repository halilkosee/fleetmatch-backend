package com.fleetmatch.offer.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateOfferRequest {

    private BigDecimal amount;

    private String message;
}