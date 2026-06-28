package com.fleetmatch.subscription.dto;

import com.fleetmatch.subscription.entity.SubscriptionPaymentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateSubscriptionPaymentStatusRequest {

    @NotNull
    private SubscriptionPaymentStatus paymentStatus;

    private String paymentProvider;

    private String externalSubscriptionId;

    private String externalCustomerId;
}
