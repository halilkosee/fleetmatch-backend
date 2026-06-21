package com.fleetmatch.offer.service;

import com.fleetmatch.common.exception.BusinessRuleException;
import com.fleetmatch.common.exception.ResourceNotFoundException;
import com.fleetmatch.company.entity.CompanyType;
import com.fleetmatch.company.entity.CompanyVerificationStatus;
import com.fleetmatch.load.entity.Load;
import com.fleetmatch.load.entity.LoadStatus;
import com.fleetmatch.load.repository.LoadRepository;
import com.fleetmatch.messaging.service.MessagingService;
import com.fleetmatch.offer.dto.CreateOfferRequest;
import com.fleetmatch.offer.dto.OfferResponse;
import com.fleetmatch.offer.entity.Offer;
import com.fleetmatch.offer.entity.OfferStatus;
import com.fleetmatch.offer.repository.OfferRepository;
import com.fleetmatch.security.user.CustomUserDetails;
import com.fleetmatch.subscription.service.SubscriptionValidationService;
import com.fleetmatch.user.entity.User;
import com.fleetmatch.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OfferService {

    private final OfferRepository offerRepository;
    private final LoadRepository loadRepository;
    private final UserRepository userRepository;
    private final SubscriptionValidationService
            subscriptionValidationService;
    private final MessagingService messagingService;

    public OfferResponse createOffer(
            UUID loadId,
            CreateOfferRequest request,
            CustomUserDetails currentUser
    ) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getCompany() == null ||
                user.getCompany().getType() != CompanyType.FLEET) {

            throw new AccessDeniedException(
                    "Only fleets can submit offers"
            );
        }

        if (user.getCompany().getVerificationStatus()
                != CompanyVerificationStatus.APPROVED) {

            throw new AccessDeniedException(
                    "Company must be approved before submitting offers"
            );
        }

        subscriptionValidationService
                .validateCanSubmitOffer(
                        user.getCompany()
                );

        Load load = loadRepository.findById(loadId)
                .orElseThrow(() -> new ResourceNotFoundException("Load not found"));

        if (load.getStatus() != LoadStatus.POSTED) {
            throw new BusinessRuleException("Offers can only be submitted for posted loads");
        }

        if (load.getBrokerCompany().getId().equals(user.getCompany().getId())) {
            throw new BusinessRuleException("You cannot submit an offer to your own load");
        }

        boolean alreadyOffered = offerRepository.existsByLoadIdAndFleetUserId(
                load.getId(),
                user.getId()
        );

        if (alreadyOffered) {
            throw new BusinessRuleException("You have already submitted an offer for this load");
        }

        Offer offer = new Offer();
        offer.setLoad(load);
        offer.setFleetUser(user);
        offer.setAmount(request.getAmount());
        offer.setMessage(request.getMessage());

        Offer saved = offerRepository.save(offer);

        return toResponse(saved);
    }

    public List<OfferResponse> getOffersForLoad(
            UUID loadId,
            CustomUserDetails currentUser
    ) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

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

    @Transactional
    public OfferResponse acceptOffer(
            UUID loadId,
            UUID offerId,
            CustomUserDetails currentUser
    ) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getCompany() == null || user.getCompany().getType() != CompanyType.BROKER) {
            throw new AccessDeniedException("Only brokers can accept offers");
        }

        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found"));

        if (!offer.getLoad().getId().equals(loadId)) {
            throw new AccessDeniedException("Offer does not belong to this load");
        }

        Load load = offer.getLoad();

        if (!load.getBrokerCompany().getId().equals(user.getCompany().getId())) {
            throw new AccessDeniedException("You can only accept offers for your own loads");
        }

        if (offer.getStatus() != OfferStatus.PENDING) {
            throw new BusinessRuleException("Only pending offers can be accepted");
        }

        if (load.getStatus() != LoadStatus.POSTED) {
            throw new BusinessRuleException("Load is not available");
        }

        offer.setStatus(OfferStatus.ACCEPTED);
        load.setStatus(LoadStatus.BOOKED);

        List<Offer> otherOffers = offerRepository.findByLoadIdAndStatus(
                load.getId(),
                OfferStatus.PENDING
        );

        for (Offer otherOffer : otherOffers) {
            if (!otherOffer.getId().equals(offer.getId())) {
                otherOffer.setStatus(OfferStatus.REJECTED);
            }
        }

        loadRepository.save(load);
        offerRepository.saveAll(otherOffers);

        Offer saved = offerRepository.save(offer);

        messagingService.createConversationForAcceptedOffer(saved);

        return toResponse(saved);
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
