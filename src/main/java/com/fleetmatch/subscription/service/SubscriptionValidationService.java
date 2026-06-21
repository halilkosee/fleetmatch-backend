package com.fleetmatch.subscription.service;

import com.fleetmatch.common.exception.BusinessRuleException;
import com.fleetmatch.company.entity.Company;
import com.fleetmatch.load.repository.LoadRepository;
import com.fleetmatch.vehicle.repository.VehicleRepository;
import com.fleetmatch.user.repository.UserRepository;
import com.fleetmatch.user.entity.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SubscriptionValidationService {

    private final SubscriptionAccessService subscriptionAccessService;

    private final VehicleRepository vehicleRepository;

    private final UserRepository userRepository;

    private final LoadRepository loadRepository;

    public void validateVehicleLimit(
            Company company
    ) {

        Integer limit =
                subscriptionAccessService
                        .getVehicleLimit(
                                company.getId()
                        );

        if (limit == null) {
            return;
        }

        long currentVehicleCount =
                vehicleRepository
                        .countByCompanyIdAndActiveTrue(
                                company.getId()
                        );

        if (currentVehicleCount >= limit) {

            throw new BusinessRuleException(
                    "Vehicle limit reached for current subscription"
            );
        }
    }

    public void validateUserLimit(
            Company company
    ) {

        Integer limit =
                subscriptionAccessService
                        .getUserLimit(
                                company.getId()
                        );

        if (limit == null) {
            return;
        }

        long currentUserCount =
                userRepository
                        .countByCompanyIdAndStatus(
                                company.getId(),
                                UserStatus.ACTIVE
                        );

        if (currentUserCount >= limit) {

            throw new BusinessRuleException(
                    "User limit reached for current subscription"
            );
        }
    }

    public void validateCanSubmitOffer(
            Company company
    ) {

        if (!subscriptionAccessService
                .canSubmitOffers(
                        company.getId()
                )) {

            throw new BusinessRuleException(
                    "Your subscription does not allow submitting offers"
            );
        }
    }

    public void validateMonthlyLoadLimit(
            Company company
    ) {

        Integer limit =
                subscriptionAccessService
                        .getMonthlyLoadLimit(
                                company.getId()
                        );

        if (limit == null) {
            return;
        }

        LocalDate firstDayOfMonth =
                LocalDate.now().withDayOfMonth(1);

        LocalDateTime start =
                firstDayOfMonth.atStartOfDay();

        LocalDateTime end =
                firstDayOfMonth
                        .plusMonths(1)
                        .atStartOfDay();

        long currentLoadCount =
                loadRepository
                        .countByBrokerCompanyIdAndCreatedAtBetween(
                                company.getId(),
                                start,
                                end
                        );

        if (currentLoadCount >= limit) {

            throw new BusinessRuleException(
                    "Monthly load limit reached for current subscription"
            );
        }
    }

    public void validateCanViewContactInfo(
            Company company
    ) {

        if (!subscriptionAccessService
                .canViewContactInfo(
                        company.getId()
                )) {

            throw new BusinessRuleException(
                    "Your subscription does not allow viewing contact information"
            );
        }
    }

    public Pageable applyLoadVisibilityLimit(
            Company company,
            Pageable pageable
    ){
        Integer limit =
                subscriptionAccessService
                        .getLoadLimit(
                                company.getId()
                        );

        if (limit == null) {
            return pageable;
        }

        return PageRequest.of(
                pageable.getPageNumber(),
                Math.min(
                        pageable.getPageSize(),
                        limit
                ),
                pageable.getSort()
        );
    }
}
