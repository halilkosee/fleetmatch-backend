package com.fleetmatch.subscription.controller;

import com.fleetmatch.subscription.dto.*;
import com.fleetmatch.subscription.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/subscriptions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SubscriptionAdminController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/plans")
    public SubscriptionPlanResponse createPlan(
            @RequestBody CreateSubscriptionPlanRequest request
    ) {

        return subscriptionService.createPlan(
                request
        );
    }

    @PutMapping("/plans/{planId}")
    public SubscriptionPlanResponse updatePlan(
            @PathVariable UUID planId,
            @RequestBody UpdateSubscriptionPlanRequest request
    ) {

        return subscriptionService.updatePlan(
                planId,
                request
        );
    }

    @GetMapping("/plans")
    public List<SubscriptionPlanResponse> getAllPlans() {

        return subscriptionService.getAllPlans();
    }

    @GetMapping("/plans/{planId}")
    public SubscriptionPlanResponse getPlan(
            @PathVariable UUID planId
    ) {

        return subscriptionService.getPlan(
                planId
        );
    }

    @PostMapping("/assign")
    public CompanySubscriptionResponse assignPlanToCompany(
            @RequestBody AssignSubscriptionRequest request
    ) {

        return subscriptionService
                .assignPlanToCompany(request);
    }

    @GetMapping("/companies")
    public List<CompanySubscriptionResponse>
    getAllCompanySubscriptions() {

        return subscriptionService
                .getAllCompanySubscriptions();
    }

    @GetMapping("/company/{companyId}")
    public CompanySubscriptionResponse
    getCompanySubscription(
            @PathVariable UUID companyId
    ) {

        return subscriptionService
                .getCompanySubscription(companyId);
    }
}
