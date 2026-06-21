package com.fleetmatch.subscription.service;

import com.fleetmatch.common.exception.ResourceNotFoundException;
import com.fleetmatch.company.entity.Company;
import com.fleetmatch.company.repository.CompanyRepository;
import com.fleetmatch.subscription.dto.*;
import com.fleetmatch.subscription.entity.CompanySubscription;
import com.fleetmatch.subscription.entity.SubscriptionPlan;
import com.fleetmatch.subscription.repository.CompanySubscriptionRepository;
import com.fleetmatch.subscription.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionPlanRepository subscriptionPlanRepository;

    private final CompanyRepository companyRepository;

    private final CompanySubscriptionRepository
            companySubscriptionRepository;

    public SubscriptionPlanResponse createPlan(
            CreateSubscriptionPlanRequest request
    ) {

        SubscriptionPlan plan = new SubscriptionPlan();

        plan.setName(request.getName());
        plan.setDescription(request.getDescription());
        plan.setMonthlyPrice(request.getMonthlyPrice());
        plan.setMaxVehicles(request.getMaxVehicles());
        plan.setMaxUsers(request.getMaxUsers());
        plan.setMaxLoadsPerMonth(request.getMaxLoadsPerMonth());
        plan.setMaxLoadsVisible(request.getMaxLoadsVisible());
        plan.setCanSubmitOffers(request.getCanSubmitOffers());
        plan.setCanViewContactInfo(
                request.getCanViewContactInfo()
        );

        SubscriptionPlan savedPlan =
                subscriptionPlanRepository.save(plan);

        return mapToResponse(savedPlan);
    }

    public SubscriptionPlanResponse updatePlan(
            UUID planId,
            UpdateSubscriptionPlanRequest request
    ) {

        SubscriptionPlan plan =
                subscriptionPlanRepository.findById(planId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Subscription plan not found"
                                ));

        plan.setName(request.getName());
        plan.setDescription(request.getDescription());
        plan.setMonthlyPrice(request.getMonthlyPrice());
        plan.setMaxVehicles(request.getMaxVehicles());
        plan.setMaxUsers(request.getMaxUsers());
        plan.setMaxLoadsPerMonth(request.getMaxLoadsPerMonth());
        plan.setMaxLoadsVisible(request.getMaxLoadsVisible());
        plan.setCanSubmitOffers(
                request.getCanSubmitOffers()
        );
        plan.setCanViewContactInfo(
                request.getCanViewContactInfo()
        );
        plan.setActive(request.getActive());

        SubscriptionPlan updatedPlan =
                subscriptionPlanRepository.save(plan);

        return mapToResponse(updatedPlan);
    }

    public SubscriptionPlanResponse getPlan(
            UUID planId
    ) {

        SubscriptionPlan plan =
                subscriptionPlanRepository.findById(planId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Subscription plan not found"
                                ));

        return mapToResponse(plan);
    }

    public List<SubscriptionPlanResponse> getAllPlans() {

        return subscriptionPlanRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private SubscriptionPlanResponse mapToResponse(
            SubscriptionPlan plan
    ) {

        return new SubscriptionPlanResponse(
                plan.getId(),
                plan.getName(),
                plan.getDescription(),
                plan.getMonthlyPrice(),
                plan.getMaxVehicles(),
                plan.getMaxUsers(),
                plan.getMaxLoadsPerMonth(),
                plan.getMaxLoadsVisible(),
                plan.getCanSubmitOffers(),
                plan.getCanViewContactInfo(),
                plan.getActive()
        );
    }

    @Transactional
    public CompanySubscriptionResponse assignPlanToCompany(
            AssignSubscriptionRequest request
    ) {

        Company company =
                companyRepository.findById(
                                request.getCompanyId()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Company not found"
                                ));

        SubscriptionPlan plan =
                subscriptionPlanRepository.findById(
                                request.getSubscriptionPlanId()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Subscription plan not found"
                                ));

        companySubscriptionRepository
                .findByCompanyIdAndActiveTrue(
                        company.getId()
                )
                .ifPresent(existing -> {

                    existing.setActive(false);

                    companySubscriptionRepository
                            .save(existing);
                });

        CompanySubscription subscription =
                new CompanySubscription();

        subscription.setCompany(company);
        subscription.setSubscriptionPlan(plan);

        subscription.setStartDate(
                request.getStartDate()
        );

        subscription.setEndDate(
                request.getEndDate()
        );

        subscription.setAutoRenew(
                request.getAutoRenew()
        );

        subscription.setCustomPrice(
                request.getCustomPrice()
        );

        subscription.setVehicleLimitOverride(
                request.getVehicleLimitOverride()
        );

        subscription.setUserLimitOverride(
                request.getUserLimitOverride()
        );

        subscription.setMonthlyLoadLimitOverride(
                request.getMonthlyLoadLimitOverride()
        );

        subscription.setLoadLimitOverride(
                request.getLoadLimitOverride()
        );

        subscription.setCanSubmitOffersOverride(
                request.getCanSubmitOffersOverride()
        );

        subscription.setCanViewContactInfoOverride(
                request.getCanViewContactInfoOverride()
        );

        CompanySubscription saved =
                companySubscriptionRepository
                        .save(subscription);

        return mapSubscription(saved);
    }

    private CompanySubscriptionResponse mapSubscription(
            CompanySubscription subscription
    ) {

        return new CompanySubscriptionResponse(
                subscription.getId(),

                subscription.getCompany().getId(),

                subscription.getCompany().getLegalName(),

                subscription.getSubscriptionPlan().getId(),

                subscription.getSubscriptionPlan().getName(),

                subscription.getStartDate(),

                subscription.getEndDate(),

                subscription.getActive(),

                subscription.getCustomPrice(),

                subscription.getVehicleLimitOverride(),

                subscription.getUserLimitOverride(),

                subscription.getMonthlyLoadLimitOverride(),

                subscription.getLoadLimitOverride()
        );
    }

    @Transactional
    public void assignFreePlan(
            Company company
    ) {

        SubscriptionPlan freePlan =
                subscriptionPlanRepository
                        .findByName("FREE")
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "FREE plan not found"
                                ));

        CompanySubscription subscription =
                new CompanySubscription();

        subscription.setCompany(company);

        subscription.setSubscriptionPlan(
                freePlan
        );

        subscription.setStartDate(
                LocalDate.now()
        );

        subscription.setActive(true);

        subscription.setAutoRenew(false);

        companySubscriptionRepository
                .save(subscription);
    }

    public CompanySubscriptionResponse getCompanySubscription(
            UUID companyId
    ) {

        CompanySubscription subscription =
                companySubscriptionRepository
                        .findByCompanyIdAndActiveTrue(
                                companyId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Subscription not found"
                                ));

        return mapSubscription(subscription);
    }

    public List<CompanySubscriptionResponse>
    getAllCompanySubscriptions() {

        return companySubscriptionRepository
                .findAll()
                .stream()
                .map(this::mapSubscription)
                .toList();
    }
}
