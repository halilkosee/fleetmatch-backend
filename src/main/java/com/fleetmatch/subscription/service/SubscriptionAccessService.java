package com.fleetmatch.subscription.service;

import com.fleetmatch.common.exception.ResourceNotFoundException;
import com.fleetmatch.subscription.entity.CompanySubscription;
import com.fleetmatch.subscription.repository.CompanySubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionAccessService {

    private final CompanySubscriptionRepository
            companySubscriptionRepository;

    public Integer getVehicleLimit(
            UUID companyId
    ) {

        CompanySubscription subscription =
                getActiveSubscription(companyId);

        if (subscription.getVehicleLimitOverride()
                != null) {

            return subscription
                    .getVehicleLimitOverride();
        }

        return subscription
                .getSubscriptionPlan()
                .getMaxVehicles();
    }

    public Integer getUserLimit(
            UUID companyId
    ) {

        CompanySubscription subscription =
                getActiveSubscription(companyId);

        if (subscription.getUserLimitOverride()
                != null) {

            return subscription
                    .getUserLimitOverride();
        }

        return subscription
                .getSubscriptionPlan()
                .getMaxUsers();
    }

    public Integer getLoadLimit(
            UUID companyId
    ) {

        CompanySubscription subscription =
                getActiveSubscription(companyId);

        if (subscription.getLoadLimitOverride()
                != null) {

            return subscription
                    .getLoadLimitOverride();
        }

        return subscription
                .getSubscriptionPlan()
                .getMaxLoadsVisible();
    }

    public Integer getMonthlyLoadLimit(
            UUID companyId
    ) {

        CompanySubscription subscription =
                getActiveSubscription(companyId);

        if (subscription.getMonthlyLoadLimitOverride()
                != null) {

            return subscription
                    .getMonthlyLoadLimitOverride();
        }

        return subscription
                .getSubscriptionPlan()
                .getMaxLoadsPerMonth();
    }

    public boolean canSubmitOffers(
            UUID companyId
    ) {

        CompanySubscription subscription =
                getActiveSubscription(companyId);

        if (subscription.getCanSubmitOffersOverride()
                != null) {

            return subscription
                    .getCanSubmitOffersOverride();
        }

        return subscription
                .getSubscriptionPlan()
                .getCanSubmitOffers();
    }

    public boolean canViewContactInfo(
            UUID companyId
    ) {

        CompanySubscription subscription =
                getActiveSubscription(companyId);

        if (subscription.getCanViewContactInfoOverride()
                != null) {

            return subscription
                    .getCanViewContactInfoOverride();
        }

        return subscription
                .getSubscriptionPlan()
                .getCanViewContactInfo();
    }

    private CompanySubscription getActiveSubscription(
            UUID companyId
    ) {

        return companySubscriptionRepository
                .findByCompanyIdAndActiveTrue(
                        companyId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "No active subscription found"
                        ));
    }
}
