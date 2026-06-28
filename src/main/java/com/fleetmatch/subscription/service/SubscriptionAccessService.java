package com.fleetmatch.subscription.service;

import com.fleetmatch.common.exception.BusinessRuleException;
import com.fleetmatch.common.exception.ResourceNotFoundException;
import com.fleetmatch.subscription.entity.CompanySubscription;
import com.fleetmatch.subscription.entity.SubscriptionPaymentStatus;
import com.fleetmatch.subscription.repository.CompanySubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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
                .map(this::requireUsableSubscription)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "No active subscription found"
                        ));
    }

    private CompanySubscription requireUsableSubscription(
            CompanySubscription subscription
    ) {
        LocalDate today = LocalDate.now();

        if (subscription.getPaymentStatus() != SubscriptionPaymentStatus.ACTIVE &&
                subscription.getPaymentStatus() != SubscriptionPaymentStatus.TRIALING) {
            throw new BusinessRuleException(
                    "Subscription payment is not active"
            );
        }

        if (Boolean.FALSE.equals(subscription.getSubscriptionPlan().getActive())) {
            throw new BusinessRuleException(
                    "Current subscription plan is not active"
            );
        }

        if (subscription.getStartDate() != null &&
                subscription.getStartDate().isAfter(today)) {
            throw new BusinessRuleException(
                    "Subscription is not active yet"
            );
        }

        if (subscription.getEndDate() != null &&
                subscription.getEndDate().isBefore(today)) {
            throw new BusinessRuleException(
                    "Subscription has expired"
            );
        }

        return subscription;
    }
}
