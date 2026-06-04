package com.fleetmatch.carrier.service;

import com.fleetmatch.common.exception.ResourceNotFoundException;
import com.fleetmatch.company.entity.CompanyType;
import com.fleetmatch.load.dto.LoadResponse;
import com.fleetmatch.offer.dto.OfferResponse;
import com.fleetmatch.offer.entity.Offer;
import com.fleetmatch.offer.repository.OfferRepository;
import com.fleetmatch.security.user.CustomUserDetails;
import com.fleetmatch.user.entity.User;
import com.fleetmatch.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.fleetmatch.load.entity.Load;
import com.fleetmatch.offer.entity.OfferStatus;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CarrierService {

    private final UserRepository userRepository;
    private final OfferRepository offerRepository;

    public Page<OfferResponse> getMyOffers(
            CustomUserDetails currentUser,
            Pageable pageable
    ) {

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getCompany() == null ||
                user.getCompany().getType() != CompanyType.CARRIER) {
            throw new AccessDeniedException("Only carriers can access this endpoint");
        }

        return offerRepository.findByCarrierUserCompanyId(
                user.getCompany().getId(),
                pageable
        ).map(this::toResponse);
    }

    private OfferResponse toResponse(Offer offer) {
        User carrier = offer.getCarrierUser();

        return new OfferResponse(
                offer.getId(),
                offer.getLoad().getId(),
                carrier.getId(),
                carrier.getFirstName() + " " + carrier.getLastName(),
                offer.getAmount(),
                offer.getMessage(),
                offer.getStatus()
        );
    }

    public List<LoadResponse> getMyAcceptedLoads(
            CustomUserDetails currentUser
    ) {

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getCompany() == null ||
                user.getCompany().getType() != CompanyType.CARRIER) {
            throw new AccessDeniedException("Only carriers can access this endpoint");
        }

        return offerRepository.findByCarrierUserCompanyIdAndStatus(
                        user.getCompany().getId(),
                        OfferStatus.ACCEPTED
                )
                .stream()
                .map(Offer::getLoad)
                .map(this::toLoadResponse)
                .toList();
    }

    private LoadResponse toLoadResponse(Load load) {
        return new LoadResponse(
                load.getId(),
                load.getPickupCity(),
                load.getPickupState(),
                load.getPickupDate(),
                load.getDeliveryCity(),
                load.getDeliveryState(),
                load.getDeliveryDate(),
                load.getEquipmentType(),
                load.getWeight(),
                load.getRate(),
                load.getMiles(),
                load.getCommodity(),
                load.getReferenceNumber(),
                load.getStatus(),
                load.getNotes(),
                load.getBrokerCompany().getLegalName()
        );
    }
}