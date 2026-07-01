package com.fleetmatch.offer.service;

import com.fleetmatch.common.exception.BusinessRuleException;
import com.fleetmatch.common.exception.ResourceNotFoundException;
import com.fleetmatch.audit.entity.AuditAction;
import com.fleetmatch.audit.service.AuditLogService;
import com.fleetmatch.company.entity.CompanyType;
import com.fleetmatch.company.entity.CompanyVerificationStatus;
import com.fleetmatch.load.entity.Load;
import com.fleetmatch.load.entity.LoadStatus;
import com.fleetmatch.load.repository.LoadRepository;
import com.fleetmatch.messaging.entity.Conversation;
import com.fleetmatch.messaging.service.MessagingService;
import com.fleetmatch.notification.event.NotificationType;
import com.fleetmatch.notification.inapp.service.NotificationService;
import com.fleetmatch.offer.dto.CreateOfferRequest;
import com.fleetmatch.offer.dto.OfferResponse;
import com.fleetmatch.offer.entity.Offer;
import com.fleetmatch.offer.entity.OfferStatus;
import com.fleetmatch.offer.repository.OfferRepository;
import com.fleetmatch.security.user.CustomUserDetails;
import com.fleetmatch.subscription.service.SubscriptionValidationService;
import com.fleetmatch.user.entity.User;
import com.fleetmatch.user.repository.UserRepository;
import com.fleetmatch.user.service.UserVerificationGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OfferService {

    private final OfferRepository offerRepository;
    private final LoadRepository loadRepository;
    private final UserRepository userRepository;
    private final SubscriptionValidationService
            subscriptionValidationService;
    private final MessagingService messagingService;
    private final UserVerificationGuard userVerificationGuard;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    @Value("${fleetmatch.workflow.fleet-confirmation-timeout-ms:14400000}")
    private long fleetConfirmationTimeoutMs = 14400000L;

    @Value("${fleetmatch.workflow.load-offer-expiration-ms:172800000}")
    private long loadOfferExpirationMs = 172800000L;

    @Transactional(noRollbackFor = BusinessRuleException.class)
    public OfferResponse createOffer(
            UUID loadId,
            CreateOfferRequest request,
            CustomUserDetails currentUser
    ) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        userVerificationGuard.requireVerifiedForCoreWorkflow(user);

        if (user.getCompany() == null ||
                user.getCompany().getType() != CompanyType.FLEET) {

            throw new AccessDeniedException(
                    "Only fleets can submit offers"
            );
        }

        if (user.getCompany().getVerificationStatus()
                != CompanyVerificationStatus.APPROVED) {

            throw new AccessDeniedException(
                    "Company must be verified before submitting offers"
            );
        }

        subscriptionValidationService
                .validateCanSubmitOffer(
                        user.getCompany()
                );

        Load load = loadRepository.findByIdForUpdate(loadId)
                .orElseThrow(() -> new ResourceNotFoundException("Load not found"));

        if (expireIfOfferDeadlinePassed(load)) {
            throw new BusinessRuleException("Load offer deadline has expired");
        }

        if (load.getStatus() != LoadStatus.POSTED) {
            throw new BusinessRuleException("Offers can only be submitted for posted loads");
        }

        if (load.getBrokerCompany().getId().equals(user.getCompany().getId())) {
            throw new BusinessRuleException("You cannot submit an offer to your own load");
        }

        boolean alreadyOffered = offerRepository.existsByLoadIdAndFleetUserCompanyIdAndStatusIn(
                load.getId(),
                user.getCompany().getId(),
                List.of(
                        OfferStatus.PENDING,
                        OfferStatus.SELECTED,
                        OfferStatus.CONFIRMED
                )
        );

        if (alreadyOffered) {
            throw new BusinessRuleException("Your company already has an active offer for this load");
        }

        Offer offer = new Offer();
        offer.setLoad(load);
        offer.setFleetUser(user);
        offer.setAmount(request.getAmount());
        offer.setMessage(request.getMessage());

        Offer saved = offerRepository.save(offer);
        notificationService.createForCompany(
                load.getBrokerCompany(),
                NotificationType.NEW_OFFER,
                "New offer",
                "A fleet submitted an offer for your load",
                "OFFER",
                saved.getId()
        );
        auditLogService.log(user, AuditAction.OFFER_SUBMITTED, "OFFER", saved.getId(), "Offer submitted");

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<OfferResponse> getOffersForLoad(
            UUID loadId,
            CustomUserDetails currentUser
    ) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        userVerificationGuard.requireVerifiedForCoreWorkflow(user);

        if (user.getCompany() == null || user.getCompany().getType() != CompanyType.BROKER) {
            throw new AccessDeniedException("Only brokers can view offers");
        }

        Load load = loadRepository.findById(loadId)
                .orElseThrow(() -> new ResourceNotFoundException("Load not found"));

        if (!load.getBrokerCompany().getId().equals(user.getCompany().getId())) {
            throw new AccessDeniedException("You can only view offers for your own loads");
        }

        return offerRepository.findByLoadId(load.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(noRollbackFor = BusinessRuleException.class)
    public OfferResponse acceptOffer(
            UUID loadId,
            UUID offerId,
            CustomUserDetails currentUser
    ) {
        return selectOffer(loadId, offerId, currentUser);
    }

    @Transactional(noRollbackFor = BusinessRuleException.class)
    public OfferResponse selectOffer(
            UUID loadId,
            UUID offerId,
            CustomUserDetails currentUser
    ) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        userVerificationGuard.requireVerifiedForCoreWorkflow(user);

        if (user.getCompany() == null || user.getCompany().getType() != CompanyType.BROKER) {
            throw new AccessDeniedException("Only brokers can select offers");
        }

        Load load = loadRepository.findByIdForUpdate(loadId)
                .orElseThrow(() -> new ResourceNotFoundException("Load not found"));

        Offer offer = offerRepository.findByIdWithLoadForUpdate(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found"));

        if (!offer.getLoad().getId().equals(loadId)) {
            throw new AccessDeniedException("Offer does not belong to this load");
        }

        if (!load.getBrokerCompany().getId().equals(user.getCompany().getId())) {
            throw new AccessDeniedException("You can only accept offers for your own loads");
        }

        if (expireIfOfferDeadlinePassed(load)) {
            throw new BusinessRuleException("Load offer deadline has expired");
        }

        if (offer.getStatus() != OfferStatus.PENDING) {
            throw new BusinessRuleException("Only pending offers can be selected");
        }

        if (load.getStatus() != LoadStatus.POSTED) {
            throw new BusinessRuleException("Load is not available");
        }

        offer.setStatus(OfferStatus.SELECTED);
        load.setStatus(LoadStatus.AWAITING_FLEET_CONFIRMATION);
        load.setConfirmationDeadlineAt(
                LocalDateTime.now().plus(Duration.ofMillis(fleetConfirmationTimeoutMs))
        );

        loadRepository.save(load);

        Offer saved = offerRepository.save(offer);

        Conversation conversation = messagingService.createConversationForSelectedOffer(saved);
        auditLogService.log(
                user,
                AuditAction.CONVERSATION_CREATED,
                "CONVERSATION",
                conversation.getId(),
                "Conversation opened for selected offer"
        );
        notificationService.createForCompany(
                saved.getFleetUser().getCompany(),
                NotificationType.OFFER_ACCEPTED,
                "Offer selected",
                "Your offer was selected by the broker",
                "OFFER",
                saved.getId()
        );
        auditLogService.log(user, AuditAction.OFFER_ACCEPTED, "OFFER", saved.getId(), "Offer selected");

        return toResponse(saved);
    }

    @Transactional
    public OfferResponse confirmAssignment(
            UUID loadId,
            UUID offerId,
            CustomUserDetails currentUser
    ) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        userVerificationGuard.requireVerifiedForCoreWorkflow(user);

        if (user.getCompany() == null ||
                user.getCompany().getType() != CompanyType.FLEET) {
            throw new AccessDeniedException("Only fleets can confirm assignments");
        }

        Load load = loadRepository.findByIdForUpdate(loadId)
                .orElseThrow(() -> new ResourceNotFoundException("Load not found"));

        Offer offer = offerRepository.findByIdWithLoadForUpdate(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found"));

        if (!offer.getLoad().getId().equals(loadId)) {
            throw new AccessDeniedException("Offer does not belong to this load");
        }

        if (!offer.getFleetUser().getCompany().getId().equals(
                user.getCompany().getId()
        )) {
            throw new AccessDeniedException("You can only confirm your own assignment");
        }

        if (isConfirmationDeadlinePassed(load)) {
            releaseTimedOutConfirmation(load, offer);
            return toResponse(offer);
        }

        if (offer.getStatus() != OfferStatus.SELECTED) {
            if (offer.getStatus() == OfferStatus.CONFIRMED &&
                    load.getStatus() == LoadStatus.BOOKED) {
                return toResponse(offer);
            }
            throw new BusinessRuleException("Only selected offers can be confirmed");
        }

        if (load.getStatus() != LoadStatus.AWAITING_FLEET_CONFIRMATION) {
            throw new BusinessRuleException("Load is not awaiting fleet confirmation");
        }

        offer.setStatus(OfferStatus.CONFIRMED);
        load.setStatus(LoadStatus.BOOKED);
        load.setConfirmationDeadlineAt(null);

        List<Offer> otherOffers = offerRepository.findByLoadIdAndStatus(
                load.getId(),
                OfferStatus.PENDING
        );

        for (Offer otherOffer : otherOffers) {
            if (!otherOffer.getId().equals(offer.getId())) {
                otherOffer.setStatus(OfferStatus.REJECTED);
                notificationService.createForCompany(
                        otherOffer.getFleetUser().getCompany(),
                        NotificationType.OFFER_REJECTED,
                        "Offer rejected",
                        "Another fleet was confirmed for this load",
                        "OFFER",
                        otherOffer.getId()
                );
                auditLogService.log(
                        user,
                        AuditAction.OFFER_REJECTED,
                        "OFFER",
                        otherOffer.getId(),
                        "Offer rejected because another fleet was confirmed"
                );
            }
        }

        loadRepository.save(load);
        offerRepository.saveAll(otherOffers);

        Offer saved = offerRepository.save(offer);
        notificationService.createForCompany(
                load.getBrokerCompany(),
                NotificationType.FLEET_CONFIRMED,
                "Fleet confirmed",
                "Fleet confirmed the assignment",
                "LOAD",
                load.getId()
        );
        auditLogService.log(
                user,
                AuditAction.OFFER_CONFIRMED,
                "OFFER",
                saved.getId(),
                "Fleet confirmed assignment"
        );

        return toResponse(saved);
    }

    @Transactional
    public OfferResponse declineAssignment(
            UUID loadId,
            UUID offerId,
            CustomUserDetails currentUser
    ) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        userVerificationGuard.requireVerifiedForCoreWorkflow(user);

        if (user.getCompany() == null ||
                user.getCompany().getType() != CompanyType.FLEET) {
            throw new AccessDeniedException("Only fleets can decline assignments");
        }

        Load load = loadRepository.findByIdForUpdate(loadId)
                .orElseThrow(() -> new ResourceNotFoundException("Load not found"));

        Offer offer = offerRepository.findByIdWithLoadForUpdate(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found"));

        if (!offer.getLoad().getId().equals(loadId)) {
            throw new AccessDeniedException("Offer does not belong to this load");
        }

        if (!offer.getFleetUser().getCompany().getId().equals(
                user.getCompany().getId()
        )) {
            throw new AccessDeniedException("You can only decline your own assignment");
        }

        if (offer.getStatus() != OfferStatus.SELECTED) {
            if (offer.getStatus() == OfferStatus.REJECTED &&
                    load.getStatus() == LoadStatus.POSTED) {
                return toResponse(offer);
            }
            throw new BusinessRuleException("Only selected offers can be declined");
        }

        if (load.getStatus() != LoadStatus.AWAITING_FLEET_CONFIRMATION) {
            throw new BusinessRuleException("Load is not awaiting fleet confirmation");
        }

        offer.setStatus(OfferStatus.REJECTED);
        load.setStatus(LoadStatus.POSTED);
        load.setConfirmationDeadlineAt(null);
        load.setOfferDeadlineAt(
                LocalDateTime.now().plus(Duration.ofMillis(loadOfferExpirationMs))
        );

        loadRepository.save(load);
        messagingService.archiveConversation(load.getId());
        notificationService.createForCompany(
                load.getBrokerCompany(),
                NotificationType.OFFER_REJECTED,
                "Fleet declined",
                "Selected fleet declined the assignment",
                "OFFER",
                offer.getId()
        );
        auditLogService.log(user, AuditAction.OFFER_REJECTED, "OFFER", offer.getId(), "Offer declined");

        return toResponse(offerRepository.save(offer));
    }

    private boolean expireIfOfferDeadlinePassed(Load load) {
        if (load.getStatus() != LoadStatus.POSTED ||
                load.getOfferDeadlineAt() == null ||
                load.getOfferDeadlineAt().isAfter(LocalDateTime.now())) {
            return false;
        }

        load.setStatus(LoadStatus.EXPIRED);
        load.setExpiredAt(LocalDateTime.now());
        load.setConfirmationDeadlineAt(null);
        loadRepository.save(load);
        notificationService.createForCompany(
                load.getBrokerCompany(),
                NotificationType.LOAD_EXPIRED,
                "Load expired",
                "Your load expired without being booked",
                "LOAD",
                load.getId()
        );
        auditLogService.log(
                null,
                AuditAction.LOAD_EXPIRED,
                "LOAD",
                load.getId(),
                "Load expired during workflow validation"
        );
        return true;
    }

    private boolean isConfirmationDeadlinePassed(Load load) {
        return load.getStatus() == LoadStatus.AWAITING_FLEET_CONFIRMATION &&
                load.getConfirmationDeadlineAt() != null &&
                !load.getConfirmationDeadlineAt().isAfter(LocalDateTime.now());
    }

    private void releaseTimedOutConfirmation(Load load, Offer offer) {
        if (offer.getStatus() == OfferStatus.SELECTED) {
            offer.setStatus(OfferStatus.REJECTED);
            offerRepository.save(offer);
            notificationService.createForCompany(
                    offer.getFleetUser().getCompany(),
                    NotificationType.FLEET_CONFIRMATION_TIMEOUT,
                    "Confirmation timed out",
                    "Your selected offer was released because it was not confirmed in time.",
                    "OFFER",
                    offer.getId()
            );
            auditLogService.log(
                    null,
                    AuditAction.OFFER_REJECTED,
                    "OFFER",
                    offer.getId(),
                    "Offer rejected during late confirmation attempt"
            );
        }

        load.setStatus(LoadStatus.POSTED);
        load.setConfirmationDeadlineAt(null);
        load.setOfferDeadlineAt(LocalDateTime.now().plus(Duration.ofMillis(loadOfferExpirationMs)));
        loadRepository.save(load);
        messagingService.archiveConversation(load.getId());
        notificationService.createForCompany(
                load.getBrokerCompany(),
                NotificationType.FLEET_CONFIRMATION_TIMEOUT,
                "Fleet confirmation timed out",
                "The selected fleet did not confirm in time. Your load is posted again.",
                "LOAD",
                load.getId()
        );
        auditLogService.log(
                null,
                AuditAction.FLEET_CONFIRMATION_TIMEOUT,
                "LOAD",
                load.getId(),
                "Fleet confirmation timed out during late confirmation attempt"
        );
    }

    private OfferResponse toResponse(Offer offer) {
        User fleetUser = offer.getFleetUser();

        return new OfferResponse(
                offer.getId(),
                offer.getLoad().getId(),
                fleetUser.getId(),
                fleetUser.getFirstName() + " " + fleetUser.getLastName(),
                offer.getAmount(),
                offer.getMessage(),
                offer.getStatus()
        );
    }
}
