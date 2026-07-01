package com.fleetmatch.subscription.service;

import com.fleetmatch.common.exception.BusinessRuleException;
import com.fleetmatch.subscription.entity.CompanySubscription;
import com.fleetmatch.subscription.entity.SubscriptionPaymentStatus;
import com.fleetmatch.subscription.entity.SubscriptionPlan;
import com.fleetmatch.subscription.repository.CompanySubscriptionRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SubscriptionAccessServiceTest {

    @Test
    void pendingPaymentSubscriptionDoesNotUnlockMarketplaceFeatures() {
        UUID companyId = UUID.randomUUID();
        SubscriptionAccessService service =
                new SubscriptionAccessService(repository(
                        subscription(SubscriptionPaymentStatus.PENDING_PAYMENT)
                ));

        assertThrows(
                BusinessRuleException.class,
                () -> service.canSubmitOffers(companyId)
        );
    }

    @Test
    void pastDueSubscriptionDoesNotUnlockMarketplaceFeatures() {
        UUID companyId = UUID.randomUUID();
        SubscriptionAccessService service =
                new SubscriptionAccessService(repository(
                        subscription(SubscriptionPaymentStatus.PAST_DUE)
                ));

        assertThrows(
                BusinessRuleException.class,
                () -> service.canViewContactInfo(companyId)
        );
    }

    @Test
    void futureSubscriptionDoesNotUnlockMarketplaceFeatures() {
        UUID companyId = UUID.randomUUID();
        CompanySubscription subscription = subscription(SubscriptionPaymentStatus.ACTIVE);
        subscription.setStartDate(LocalDate.now().plusDays(1));
        SubscriptionAccessService service =
                new SubscriptionAccessService(repository(subscription));

        assertThrows(
                BusinessRuleException.class,
                () -> service.canSubmitOffers(companyId)
        );
    }

    @Test
    void expiredSubscriptionDoesNotUnlockMarketplaceFeatures() {
        UUID companyId = UUID.randomUUID();
        CompanySubscription subscription = subscription(SubscriptionPaymentStatus.ACTIVE);
        subscription.setEndDate(LocalDate.now().minusDays(1));
        SubscriptionAccessService service =
                new SubscriptionAccessService(repository(subscription));

        assertThrows(
                BusinessRuleException.class,
                () -> service.canSubmitOffers(companyId)
        );
    }

    @Test
    void activeSubscriptionUnlocksMarketplaceFeatures() {
        UUID companyId = UUID.randomUUID();
        SubscriptionAccessService service =
                new SubscriptionAccessService(repository(
                        subscription(SubscriptionPaymentStatus.ACTIVE)
                ));

        assertDoesNotThrow(() -> service.canSubmitOffers(companyId));
    }

    @Test
    void trialingSubscriptionUnlocksMarketplaceFeatures() {
        UUID companyId = UUID.randomUUID();
        SubscriptionAccessService service =
                new SubscriptionAccessService(repository(
                        subscription(SubscriptionPaymentStatus.TRIALING)
                ));

        assertDoesNotThrow(() -> service.canSubmitOffers(companyId));
    }

    private CompanySubscriptionRepository repository(
            CompanySubscription subscription
    ) {
        return (CompanySubscriptionRepository) Proxy.newProxyInstance(
                CompanySubscriptionRepository.class.getClassLoader(),
                new Class<?>[]{CompanySubscriptionRepository.class},
                (proxy, method, args) -> {
                    if ("findByCompanyIdAndActiveTrue".equals(method.getName())) {
                        return Optional.of(subscription);
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private CompanySubscription subscription(SubscriptionPaymentStatus status) {
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setName("STARTER");
        plan.setMonthlyPrice(BigDecimal.valueOf(49));
        plan.setCanSubmitOffers(true);
        plan.setCanViewContactInfo(true);
        plan.setActive(true);

        CompanySubscription subscription = new CompanySubscription();
        subscription.setSubscriptionPlan(plan);
        subscription.setActive(true);
        subscription.setPaymentStatus(status);
        return subscription;
    }
}
