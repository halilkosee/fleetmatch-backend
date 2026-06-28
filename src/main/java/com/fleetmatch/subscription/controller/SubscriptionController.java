package com.fleetmatch.subscription.controller;

import com.fleetmatch.security.user.CustomUserDetails;
import com.fleetmatch.subscription.dto.CompanySubscriptionResponse;
import com.fleetmatch.subscription.dto.SubscriptionPlanResponse;
import com.fleetmatch.subscription.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping("/plans")
    public List<SubscriptionPlanResponse> getAvailablePlans(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return subscriptionService.getAvailablePlansForApprovedCompany(currentUser);
    }

    @PostMapping("/plans/{planId}/select")
    public CompanySubscriptionResponse selectPlan(
            @PathVariable UUID planId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return subscriptionService.selectPlanForApprovedCompany(planId, currentUser);
    }
}
