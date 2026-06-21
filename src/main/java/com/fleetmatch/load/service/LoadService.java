package com.fleetmatch.load.service;

import com.fleetmatch.common.exception.BusinessRuleException;
import com.fleetmatch.common.exception.ResourceNotFoundException;
import com.fleetmatch.company.entity.CompanyType;
import com.fleetmatch.company.entity.CompanyVerificationStatus;
import com.fleetmatch.load.dto.CreateLoadRequest;
import com.fleetmatch.load.dto.LoadResponse;
import com.fleetmatch.load.entity.EquipmentType;
import com.fleetmatch.load.entity.Load;
import com.fleetmatch.load.entity.LoadStatus;
import com.fleetmatch.load.repository.LoadRepository;
import com.fleetmatch.offer.entity.Offer;
import com.fleetmatch.offer.entity.OfferStatus;
import com.fleetmatch.offer.repository.OfferRepository;
import com.fleetmatch.security.user.CustomUserDetails;
import com.fleetmatch.subscription.service.SubscriptionAccessService;
import com.fleetmatch.user.entity.PlatformRole;
import com.fleetmatch.user.entity.User;
import com.fleetmatch.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoadService {

    private final LoadRepository loadRepository;
    private final UserRepository userRepository;
    private final OfferRepository offerRepository;
    private final SubscriptionAccessService
            subscriptionAccessService;

    public LoadResponse createLoad(
            CreateLoadRequest request,
            CustomUserDetails currentUser
    ) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getCompany() == null ||
                user.getCompany().getType() != CompanyType.BROKER) {
            throw new AccessDeniedException("Only brokers can create loads");
        }

        if (user.getCompany().getVerificationStatus()
                != CompanyVerificationStatus.APPROVED) {

            throw new AccessDeniedException(
                    "Company must be approved before creating loads"
            );
        }

        Load load = new Load();
        load.setBrokerCompany(user.getCompany());
        load.setCreatedBy(user);

        load.setPickupCity(request.getPickupCity());
        load.setPickupState(request.getPickupState());
        load.setPickupDate(request.getPickupDate());

        load.setDeliveryCity(request.getDeliveryCity());
        load.setDeliveryState(request.getDeliveryState());
        load.setDeliveryDate(request.getDeliveryDate());

        load.setEquipmentType(request.getEquipmentType());
        load.setWeight(request.getWeight());
        load.setRate(request.getRate());
        load.setMiles(request.getMiles());
        load.setCommodity(request.getCommodity());
        load.setReferenceNumber(request.getReferenceNumber());
        load.setNotes(request.getNotes());

        Load saved = loadRepository.save(load);

        return toResponse(
                saved,
                user
        );
    }

    public Page<LoadResponse> getPostedLoads(
            Pageable pageable,
            CustomUserDetails currentUser
    ) {

        User user = userRepository.findById(
                currentUser.getId()
        ).orElseThrow(() ->
                new ResourceNotFoundException(
                        "User not found"
                ));

        return loadRepository.findByStatus(
                LoadStatus.POSTED,
                pageable
        ).map(load -> toResponse(
                load,
                user
        ));
    }

    public LoadResponse getLoadById(
            UUID loadId,
            CustomUserDetails currentUser
    ) {
        Load load = loadRepository.findById(loadId)
                .orElseThrow(() -> new ResourceNotFoundException("Load not found"));
        User user = userRepository.findById(
                currentUser.getId()
        ).orElseThrow(() ->
                new ResourceNotFoundException(
                        "User not found"
                ));

        requireCanViewLoad(
                load,
                user
        );

        return toResponse(
                load,
                user
        );
    }

    public List<LoadResponse> searchLoads(
            String pickupState,
            String deliveryState,
            EquipmentType equipmentType,
            CustomUserDetails currentUser
    ) {
        User user = userRepository.findById(
                currentUser.getId()
        ).orElseThrow(() ->
                new ResourceNotFoundException(
                        "User not found"
                ));

        List<Load> loads;

        if (pickupState != null && deliveryState != null && equipmentType != null) {
            loads = loadRepository
                    .findByStatusAndPickupStateIgnoreCaseAndDeliveryStateIgnoreCaseAndEquipmentType(
                            LoadStatus.POSTED,
                            pickupState,
                            deliveryState,
                            equipmentType
                    );
        } else if (pickupState != null) {
            loads = loadRepository.findByStatusAndPickupStateIgnoreCase(
                    LoadStatus.POSTED,
                    pickupState
            );
        } else if (deliveryState != null) {
            loads = loadRepository.findByStatusAndDeliveryStateIgnoreCase(
                    LoadStatus.POSTED,
                    deliveryState
            );
        } else if (equipmentType != null) {
            loads = loadRepository.findByStatusAndEquipmentType(
                    LoadStatus.POSTED,
                    equipmentType
            );
        } else {
            loads = loadRepository.findByStatus(LoadStatus.POSTED);
        }

        return loads.stream()
                .map(load -> toResponse(
                load,
                user
        ))
                .toList();
    }

    @Transactional
    public LoadResponse startLoad(UUID loadId, CustomUserDetails currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Load load = loadRepository.findById(loadId)
                .orElseThrow(() -> new ResourceNotFoundException("Load not found"));

        if (load.getStatus() != LoadStatus.BOOKED) {
            throw new BusinessRuleException("Only booked loads can be started");
        }

        if (user.getPlatformRole() != PlatformRole.ADMIN) {
            Offer acceptedOffer = offerRepository.findFirstByLoadIdAndStatus(
                    load.getId(),
                    OfferStatus.ACCEPTED
            ).orElseThrow(() -> new ResourceNotFoundException("Accepted offer not found"));

            if (user.getCompany() == null ||
                    !acceptedOffer.getFleetUser().getCompany().getId().equals(user.getCompany().getId())) {
                throw new AccessDeniedException("Only the accepted fleet driver can start this load");
            }
        }

        load.setStatus(LoadStatus.IN_TRANSIT);

        return toResponse(
                load,
                user
        );
    }

    @Transactional
    public LoadResponse deliverLoad(UUID loadId, CustomUserDetails currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Load load = loadRepository.findById(loadId)
                .orElseThrow(() -> new ResourceNotFoundException("Load not found"));

        if (load.getStatus() != LoadStatus.IN_TRANSIT) {
            throw new BusinessRuleException("Only in-transit loads can be delivered");
        }

        if (user.getPlatformRole() != PlatformRole.ADMIN) {
            Offer acceptedOffer = offerRepository.findFirstByLoadIdAndStatus(
                    load.getId(),
                    OfferStatus.ACCEPTED
            ).orElseThrow(() -> new ResourceNotFoundException("Accepted offer not found"));

            if (user.getCompany() == null ||
                    !acceptedOffer.getFleetUser().getCompany().getId().equals(user.getCompany().getId())) {
                throw new AccessDeniedException("Only the accepted fleet driver can deliver this load");
            }
        }

        load.setStatus(LoadStatus.DELIVERED);

        return toResponse(
                load,
                user
        );
    }

    @Transactional
    public LoadResponse cancelLoad(UUID loadId, CustomUserDetails currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Load load = loadRepository.findById(loadId)
                .orElseThrow(() -> new ResourceNotFoundException("Load not found"));

        if (load.getStatus() == LoadStatus.DELIVERED) {
            throw new BusinessRuleException("Delivered loads cannot be cancelled");
        }

        if (load.getStatus() == LoadStatus.CANCELLED) {
            throw new BusinessRuleException("Load is already cancelled");
        }

        if (user.getPlatformRole() != PlatformRole.ADMIN) {
            if (user.getCompany() == null ||
                    user.getCompany().getType() != CompanyType.BROKER ||
                    !load.getBrokerCompany().getId().equals(user.getCompany().getId())) {
                throw new AccessDeniedException("Only the broker owner of this load can cancel it");
            }
        }

        load.setStatus(LoadStatus.CANCELLED);

        return toResponse(
                load,
                user
        );
    }

    private void requireCanViewLoad(
            Load load,
            User user
    ) {
        if (load.getStatus() == LoadStatus.POSTED ||
                user.getPlatformRole() == PlatformRole.ADMIN) {
            return;
        }

        if (user.getCompany() == null) {
            throw new AccessDeniedException(
                    "You do not have access to this load"
            );
        }

        if (load.getBrokerCompany().getId().equals(
                user.getCompany().getId()
        )) {
            return;
        }

        Offer acceptedOffer = offerRepository.findFirstByLoadIdAndStatus(
                load.getId(),
                OfferStatus.ACCEPTED
        ).orElseThrow(() -> new AccessDeniedException(
                "You do not have access to this load"
        ));

        if (!acceptedOffer.getFleetUser().getCompany().getId().equals(
                user.getCompany().getId()
        )) {
            throw new AccessDeniedException(
                    "You do not have access to this load"
            );
        }
    }

    private LoadResponse toResponse(
            Load load,
            User viewer
    ) {
        String brokerEmail = null;
        String brokerPhone = null;

        if (viewer != null &&
                viewer.getCompany() != null &&
                subscriptionAccessService
                        .canViewContactInfo(
                                viewer.getCompany().getId()
                        )) {

            brokerEmail =
                    load.getBrokerCompany().getEmail();

            brokerPhone =
                    load.getBrokerCompany().getPhone();
        }

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

                load.getBrokerCompany().getLegalName(),
                brokerEmail,
                brokerPhone
        );
    }
}
