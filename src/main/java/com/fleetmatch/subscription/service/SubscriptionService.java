package com.fleetmatch.subscription.service;

import com.fleetmatch.audit.entity.AuditAction;
import com.fleetmatch.audit.service.AuditLogService;
import com.fleetmatch.common.exception.BusinessRuleException;
import com.fleetmatch.common.exception.ResourceNotFoundException;
import com.fleetmatch.company.entity.Company;
import com.fleetmatch.company.entity.CompanyVerificationStatus;
import com.fleetmatch.notification.event.NotificationType;
import com.fleetmatch.notification.inapp.service.NotificationService;
import com.fleetmatch.company.repository.CompanyRepository;
import com.fleetmatch.security.user.CustomUserDetails;
import com.fleetmatch.subscription.dto.*;
import com.fleetmatch.subscription.entity.CompanySubscription;
import com.fleetmatch.subscription.entity.SubscriptionPlan;
import com.fleetmatch.subscription.entity.SubscriptionPaymentStatus;
import com.fleetmatch.subscription.repository.CompanySubscriptionRepository;
import com.fleetmatch.subscription.repository.SubscriptionPlanRepository;
import com.fleetmatch.user.entity.User;
import com.fleetmatch.user.entity.UserStatus;
import com.fleetmatch.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionService {

    private final SubscriptionPlanRepository subscriptionPlanRepository;

    private final CompanyRepository companyRepository;

    private final CompanySubscriptionRepository
            companySubscriptionRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    @Transactional
    public SubscriptionPlanResponse createPlan(
            CreateSubscriptionPlanRequest request
    ) {
        return createPlan(request, null);
    }

    @Transactional
    public SubscriptionPlanResponse createPlan(
            CreateSubscriptionPlanRequest request,
            CustomUserDetails currentUser
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
        auditLogService.log(
                getActor(currentUser),
                AuditAction.SUBSCRIPTION_PLAN_CREATED,
                "SUBSCRIPTION_PLAN",
                savedPlan.getId(),
                "Subscription plan created"
        );

        return mapToResponse(savedPlan);
    }

    @Transactional
    public SubscriptionPlanResponse updatePlan(
            UUID planId,
            UpdateSubscriptionPlanRequest request
    ) {
        return updatePlan(planId, request, null);
    }

    @Transactional
    public SubscriptionPlanResponse updatePlan(
            UUID planId,
            UpdateSubscriptionPlanRequest request,
            CustomUserDetails currentUser
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
        auditLogService.log(
                getActor(currentUser),
                AuditAction.SUBSCRIPTION_PLAN_UPDATED,
                "SUBSCRIPTION_PLAN",
                updatedPlan.getId(),
                "Subscription plan updated"
        );

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

    public List<SubscriptionPlanResponse> getAvailablePlansForApprovedCompany(
            CustomUserDetails currentUser
    ) {
        requireApprovedSubscriptionSelectionUser(currentUser);

        return subscriptionPlanRepository.findByActiveTrue()
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
        return assignPlanToCompany(request, null);
    }

    @Transactional
    public CompanySubscriptionResponse assignPlanToCompany(
            AssignSubscriptionRequest request,
            CustomUserDetails currentUser
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
        subscription.setActive(true);
        subscription.setPaymentStatus(SubscriptionPaymentStatus.ACTIVE);

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
        notificationService.createForCompany(
                company,
                NotificationType.SUBSCRIPTION_ASSIGNED,
                "Subscription assigned",
                "A subscription plan was assigned to your company",
                "SUBSCRIPTION",
                saved.getId()
        );
        auditLogService.log(
                getActor(currentUser),
                AuditAction.SUBSCRIPTION_ASSIGNED,
                "SUBSCRIPTION",
                saved.getId(),
                "Subscription assigned"
        );
        auditLogService.log(
                getActor(currentUser),
                AuditAction.SUBSCRIPTION_CHANGED,
                "COMPANY",
                company.getId(),
                "Company subscription changed to plan " + plan.getName()
        );

        return mapSubscription(saved);
    }

    @Transactional
    public CompanySubscriptionResponse selectPlanForApprovedCompany(
            UUID planId,
            CustomUserDetails currentUser
    ) {
        User user = requireApprovedSubscriptionSelectionUser(currentUser);
        Company company = user.getCompany();

        SubscriptionPlan plan =
                subscriptionPlanRepository.findById(planId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Subscription plan not found"
                                ));

        if (!plan.getActive()) {
            throw new BusinessRuleException("Subscription plan is not available");
        }

        companySubscriptionRepository
                .findByCompanyIdAndActiveTrue(company.getId())
                .ifPresent(existing -> {
                    existing.setActive(false);
                    companySubscriptionRepository.save(existing);
                });

        CompanySubscription subscription = new CompanySubscription();
        subscription.setCompany(company);
        subscription.setSubscriptionPlan(plan);
        subscription.setStartDate(LocalDate.now());
        subscription.setAutoRenew(false);
        if (plan.getMonthlyPrice() != null &&
                plan.getMonthlyPrice().signum() > 0) {
            subscription.setActive(false);
            subscription.setPaymentStatus(SubscriptionPaymentStatus.PENDING_PAYMENT);
        } else {
            subscription.setActive(true);
            subscription.setPaymentStatus(SubscriptionPaymentStatus.ACTIVE);
        }

        CompanySubscription saved = companySubscriptionRepository.save(subscription);
        auditLogService.log(
                user,
                AuditAction.SUBSCRIPTION_SELECTED,
                "SUBSCRIPTION",
                saved.getId(),
                "Company selected subscription plan " + plan.getName()
        );
        auditLogService.log(
                user,
                AuditAction.SUBSCRIPTION_CHANGED,
                "COMPANY",
                company.getId(),
                "Company subscription changed to plan " + plan.getName()
        );
        notificationService.createForCompany(
                company,
                NotificationType.SUBSCRIPTION_ASSIGNED,
                "Subscription selected",
                "Your subscription plan is now active",
                "SUBSCRIPTION",
                saved.getId()
        );

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

                subscription.getPaymentStatus(),

                subscription.getPaymentProvider(),

                subscription.getExternalSubscriptionId(),

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

    @Transactional
    public CompanySubscriptionResponse updatePaymentStatus(
            UUID companyId,
            UpdateSubscriptionPaymentStatusRequest request,
            CustomUserDetails currentUser
    ) {
        CompanySubscription subscription =
                companySubscriptionRepository
                        .findTopByCompanyIdOrderByCreatedAtDesc(companyId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Subscription not found"
                                ));

        subscription.setPaymentStatus(request.getPaymentStatus());
        subscription.setPaymentProvider(request.getPaymentProvider());
        subscription.setExternalSubscriptionId(request.getExternalSubscriptionId());
        subscription.setExternalCustomerId(request.getExternalCustomerId());
        subscription.setActive(
                request.getPaymentStatus() == SubscriptionPaymentStatus.ACTIVE ||
                        request.getPaymentStatus() == SubscriptionPaymentStatus.TRIALING
        );

        CompanySubscription saved =
                companySubscriptionRepository.save(subscription);
        auditLogService.log(
                getActor(currentUser),
                AuditAction.SUBSCRIPTION_CHANGED,
                "SUBSCRIPTION",
                saved.getId(),
                "Subscription payment status changed to " + saved.getPaymentStatus()
        );

        return mapSubscription(saved);
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

    private User getActor(CustomUserDetails currentUser) {
        if (currentUser == null) {
            return null;
        }

        return userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private User requireApprovedSubscriptionSelectionUser(CustomUserDetails currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getCompany() == null) {
            throw new BusinessRuleException("Company is required before selecting a subscription");
        }

        if (user.getCompany().getVerificationStatus() != CompanyVerificationStatus.APPROVED ||
                (user.getStatus() != UserStatus.APPROVED &&
                        user.getStatus() != UserStatus.ACTIVE)) {
            throw new BusinessRuleException("Company must be approved before selecting a subscription");
        }

        return user;
    }
}
