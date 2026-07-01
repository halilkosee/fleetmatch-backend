package com.fleetmatch.load.service;

import com.fleetmatch.common.exception.BusinessRuleException;
import com.fleetmatch.common.exception.ResourceNotFoundException;
import com.fleetmatch.audit.entity.AuditAction;
import com.fleetmatch.audit.service.AuditLogService;
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
import com.fleetmatch.notification.event.NotificationType;
import com.fleetmatch.notification.inapp.service.NotificationService;
import com.fleetmatch.security.user.CustomUserDetails;
import com.fleetmatch.subscription.service.SubscriptionAccessService;
import com.fleetmatch.subscription.service.SubscriptionValidationService;
import com.fleetmatch.user.entity.PlatformRole;
import com.fleetmatch.user.entity.User;
import com.fleetmatch.user.repository.UserRepository;
import com.fleetmatch.user.service.UserVerificationGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class LoadService {

    private final LoadRepository loadRepository;
    private final UserRepository userRepository;
    private final OfferRepository offerRepository;
    private final SubscriptionAccessService
            subscriptionAccessService;
    private final SubscriptionValidationService
            subscriptionValidationService;
    private final UserVerificationGuard userVerificationGuard;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    @Transactional
    public LoadResponse createLoad(
            CreateLoadRequest request,
            CustomUserDetails currentUser
    ) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        userVerificationGuard.requireVerifiedForCoreWorkflow(user);

        if (user.getCompany() == null ||
                user.getCompany().getType() != CompanyType.BROKER) {
            throw new AccessDeniedException("Only brokers can create loads");
        }

        if (user.getCompany().getVerificationStatus()
                != CompanyVerificationStatus.APPROVED) {

            throw new AccessDeniedException(
                    "Company must be verified before creating loads"
            );
        }

        subscriptionValidationService
                .validateMonthlyLoadLimit(
                        user.getCompany()
                );

        if (request.getWeight() == null && request.getWeightLbs() == null) {
            throw new BusinessRuleException("weightLbs is required");
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
        load.setWeight(request.getWeight() == null ? request.getWeightLbs() : request.getWeight());
        load.setWeightLbs(request.getWeightLbs() == null ? request.getWeight() : request.getWeightLbs());
        load.setRate(request.getRate());
        load.setMiles(request.getMiles());
        load.setCommodity(request.getCommodity());
        load.setReferenceNumber(request.getReferenceNumber());
        load.setNotes(request.getNotes());
        applyAdvancedFields(load, request);

        Load saved = loadRepository.save(load);
        auditLogService.log(user, AuditAction.LOAD_CREATED, "LOAD", saved.getId(), "Load created");

        return toResponse(
                saved,
                user
        );
    }

    @Transactional(readOnly = true)
    public Page<LoadResponse> searchLoadsPaged(
            String pickupState,
            String deliveryState,
            EquipmentType equipmentType,
            LocalDate pickupDateFrom,
            LocalDate pickupDateTo,
            Integer minWeight,
            Integer maxWeight,
            BigDecimal minRate,
            BigDecimal maxRate,
            LoadStatus status,
            String keyword,
            Pageable pageable,
            CustomUserDetails currentUser
    ) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        userVerificationGuard.requireVerifiedForCoreWorkflow(user);

        Specification<Load> specification = dynamicSearchSpecification(
                pickupState,
                deliveryState,
                equipmentType,
                pickupDateFrom,
                pickupDateTo,
                minWeight,
                maxWeight,
                minRate,
                maxRate,
                status == null ? LoadStatus.POSTED : status,
                keyword
        );

        Pageable limitedPageable = subscriptionValidationService
                .applyLoadVisibilityLimit(user.getCompany(), pageable);

        return loadRepository.findAll(specification, limitedPageable)
                .map(load -> toResponse(load, user));
    }

    @Transactional
    public LoadResponse duplicateLoad(UUID loadId, CustomUserDetails currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        userVerificationGuard.requireVerifiedForCoreWorkflow(user);

        Load source = loadRepository.findById(loadId)
                .orElseThrow(() -> new ResourceNotFoundException("Load not found"));

        if (user.getCompany() == null ||
                user.getCompany().getType() != CompanyType.BROKER ||
                !source.getBrokerCompany().getId().equals(user.getCompany().getId())) {
            throw new AccessDeniedException("Only the broker company that owns this load can duplicate it");
        }

        subscriptionValidationService.validateMonthlyLoadLimit(user.getCompany());

        Load duplicate = new Load();
        duplicate.setBrokerCompany(source.getBrokerCompany());
        duplicate.setCreatedBy(user);
        duplicate.setPickupCity(source.getPickupCity());
        duplicate.setPickupState(source.getPickupState());
        duplicate.setPickupDate(source.getPickupDate());
        duplicate.setDeliveryCity(source.getDeliveryCity());
        duplicate.setDeliveryState(source.getDeliveryState());
        duplicate.setDeliveryDate(source.getDeliveryDate());
        duplicate.setEquipmentType(source.getEquipmentType());
        duplicate.setWeight(source.getWeight());
        duplicate.setWeightLbs(source.getWeightLbs());
        duplicate.setRate(source.getRate());
        duplicate.setMiles(source.getMiles());
        duplicate.setCommodity(source.getCommodity());
        duplicate.setReferenceNumber(source.getReferenceNumber() + "-COPY");
        duplicate.setNotes(source.getNotes());
        copyAdvancedFields(source, duplicate);
        duplicate.setStatus(LoadStatus.POSTED);

        Load saved = loadRepository.save(duplicate);
        auditLogService.log(
                user,
                AuditAction.LOAD_DUPLICATED,
                "LOAD",
                saved.getId(),
                "Load duplicated from " + source.getId()
        );
        return toResponse(saved, user);
    }

    @Transactional(readOnly = true)
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

        userVerificationGuard.requireVerifiedForCoreWorkflow(user);

        Pageable limitedPageable =
                subscriptionValidationService
                        .applyLoadVisibilityLimit(
                                user.getCompany(),
                                pageable
                        );

        return loadRepository.findByStatus(
                LoadStatus.POSTED,
                limitedPageable
        ).map(load -> toResponse(
                load,
                user
        ));
    }

    @Transactional(readOnly = true)
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

        userVerificationGuard.requireVerifiedForCoreWorkflow(user);

        return toResponse(
                load,
                user
        );
    }

    @Transactional(readOnly = true)
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

        userVerificationGuard.requireVerifiedForCoreWorkflow(user);

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

        Integer visibleLoadLimit =
                subscriptionAccessService.getLoadLimit(
                        user.getCompany().getId()
                );

        return loads.stream()
                .limit(visibleLoadLimit == null
                        ? Long.MAX_VALUE
                        : visibleLoadLimit)
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

        userVerificationGuard.requireVerifiedForCoreWorkflow(user);

        Load load = loadRepository.findByIdForUpdate(loadId)
                .orElseThrow(() -> new ResourceNotFoundException("Load not found"));

        if (user.getPlatformRole() != PlatformRole.ADMIN) {
            Offer acceptedOffer = offerRepository.findFirstByLoadIdAndStatus(
                    load.getId(),
                    OfferStatus.CONFIRMED
            ).orElseThrow(() -> new ResourceNotFoundException("Confirmed offer not found"));

            if (user.getCompany() == null ||
                    !acceptedOffer.getFleetUser().getCompany().getId().equals(user.getCompany().getId())) {
                throw new AccessDeniedException("Only the accepted fleet driver can start this load");
            }
        }

        if (load.getStatus() == LoadStatus.IN_TRANSIT) {
            return toResponse(load, user);
        }

        if (load.getStatus() != LoadStatus.BOOKED) {
            throw new BusinessRuleException("Only booked loads can be started");
        }

        load.setStatus(LoadStatus.IN_TRANSIT);
        notificationService.createForCompany(
                load.getBrokerCompany(),
                NotificationType.LOAD_STARTED,
                "Load started",
                "A load has started",
                "LOAD",
                load.getId()
        );

        Load saved = loadRepository.save(load);
        auditLogService.log(user, AuditAction.LOAD_STARTED, "LOAD", saved.getId(), "Load started");

        return toResponse(
                saved,
                user
        );
    }

    @Transactional
    public LoadResponse deliverLoad(UUID loadId, CustomUserDetails currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        userVerificationGuard.requireVerifiedForCoreWorkflow(user);

        Load load = loadRepository.findByIdForUpdate(loadId)
                .orElseThrow(() -> new ResourceNotFoundException("Load not found"));

        if (user.getPlatformRole() != PlatformRole.ADMIN) {
            Offer acceptedOffer = offerRepository.findFirstByLoadIdAndStatus(
                    load.getId(),
                    OfferStatus.CONFIRMED
            ).orElseThrow(() -> new ResourceNotFoundException("Confirmed offer not found"));

            if (user.getCompany() == null ||
                    !acceptedOffer.getFleetUser().getCompany().getId().equals(user.getCompany().getId())) {
                throw new AccessDeniedException("Only the accepted fleet driver can deliver this load");
            }
        }

        if (load.getStatus() == LoadStatus.DELIVERED) {
            return toResponse(load, user);
        }

        if (load.getStatus() != LoadStatus.IN_TRANSIT) {
            throw new BusinessRuleException("Only in-transit loads can be delivered");
        }

        load.setStatus(LoadStatus.DELIVERED);
        notificationService.createForCompany(
                load.getBrokerCompany(),
                NotificationType.LOAD_DELIVERED,
                "Load delivered",
                "A load has been delivered",
                "LOAD",
                load.getId()
        );

        Load saved = loadRepository.save(load);
        auditLogService.log(user, AuditAction.LOAD_DELIVERED, "LOAD", saved.getId(), "Load delivered");

        return toResponse(
                saved,
                user
        );
    }

    @Transactional
    public LoadResponse cancelLoad(UUID loadId, CustomUserDetails currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Load load = loadRepository.findByIdForUpdate(loadId)
                .orElseThrow(() -> new ResourceNotFoundException("Load not found"));

        if (user.getPlatformRole() != PlatformRole.ADMIN) {
            userVerificationGuard.requireVerifiedForCoreWorkflow(user);

            if (user.getCompany() == null ||
                    user.getCompany().getType() != CompanyType.BROKER ||
                    !load.getBrokerCompany().getId().equals(user.getCompany().getId())) {
                throw new AccessDeniedException("Only the broker owner of this load can cancel it");
            }
        }

        if (load.getStatus() == LoadStatus.CANCELLED) {
            return toResponse(load, user);
        }

        if (load.getStatus() == LoadStatus.IN_TRANSIT ||
                load.getStatus() == LoadStatus.DELIVERED) {
            throw new BusinessRuleException("In-transit or delivered loads cannot be cancelled");
        }

        load.setStatus(LoadStatus.CANCELLED);
        notificationService.createForCompany(
                load.getBrokerCompany(),
                NotificationType.LOAD_CANCELLED,
                "Load cancelled",
                "A load has been cancelled",
                "LOAD",
                load.getId()
        );
        notifyFleetAboutCancellation(load, user);

        Load saved = loadRepository.save(load);
        auditLogService.log(user, AuditAction.LOAD_CANCELLED, "LOAD", saved.getId(), "Load cancelled");

        return toResponse(
                saved,
                user
        );
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
                brokerPhone,
                load.getWeightLbs(),
                load.getDescription(),
                load.getPickupStreetAddress(),
                load.getPickupZipCode(),
                load.getPickupLocationName(),
                load.getPickupContactName(),
                load.getPickupContactPhone(),
                load.getPickupTimeWindowStart(),
                load.getPickupTimeWindowEnd(),
                load.getPickupInstructions(),
                load.getDeliveryStreetAddress(),
                load.getDeliveryZipCode(),
                load.getDeliveryLocationName(),
                load.getDeliveryContactName(),
                load.getDeliveryContactPhone(),
                load.getDeliveryTimeWindowStart(),
                load.getDeliveryTimeWindowEnd(),
                load.getDeliveryInstructions(),
                load.getPalletCount(),
                load.getPieceCount(),
                load.getLengthInches(),
                load.getWidthInches(),
                load.getHeightInches(),
                load.isLiftgateRequired(),
                load.isPalletJackRequired(),
                load.isDockHighRequired(),
                load.isResidentialDelivery()
        );
    }

    private Specification<Load> dynamicSearchSpecification(
            String pickupState,
            String deliveryState,
            EquipmentType equipmentType,
            LocalDate pickupDateFrom,
            LocalDate pickupDateTo,
            Integer minWeight,
            Integer maxWeight,
            BigDecimal minRate,
            BigDecimal maxRate,
            LoadStatus status,
            String keyword
    ) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (pickupState != null && !pickupState.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("pickupState")), pickupState.toLowerCase()));
            }
            if (deliveryState != null && !deliveryState.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("deliveryState")), deliveryState.toLowerCase()));
            }
            if (equipmentType != null) {
                predicates.add(cb.equal(root.get("equipmentType"), equipmentType));
            }
            if (pickupDateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("pickupDate"), pickupDateFrom));
            }
            if (pickupDateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("pickupDate"), pickupDateTo));
            }
            if (minWeight != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("weightLbs"), minWeight));
            }
            if (maxWeight != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("weightLbs"), maxWeight));
            }
            if (minRate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("rate"), minRate));
            }
            if (maxRate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("rate"), maxRate));
            }
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("commodity")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern),
                        cb.like(cb.lower(root.get("referenceNumber")), pattern)
                ));
            }

            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private void applyAdvancedFields(Load load, CreateLoadRequest request) {
        load.setDescription(request.getDescription());
        load.setPickupStreetAddress(request.getPickupStreetAddress());
        load.setPickupZipCode(request.getPickupZipCode());
        load.setPickupLocationName(request.getPickupLocationName());
        load.setPickupContactName(request.getPickupContactName());
        load.setPickupContactPhone(request.getPickupContactPhone());
        load.setPickupTimeWindowStart(request.getPickupTimeWindowStart());
        load.setPickupTimeWindowEnd(request.getPickupTimeWindowEnd());
        load.setPickupInstructions(request.getPickupInstructions());
        load.setDeliveryStreetAddress(request.getDeliveryStreetAddress());
        load.setDeliveryZipCode(request.getDeliveryZipCode());
        load.setDeliveryLocationName(request.getDeliveryLocationName());
        load.setDeliveryContactName(request.getDeliveryContactName());
        load.setDeliveryContactPhone(request.getDeliveryContactPhone());
        load.setDeliveryTimeWindowStart(request.getDeliveryTimeWindowStart());
        load.setDeliveryTimeWindowEnd(request.getDeliveryTimeWindowEnd());
        load.setDeliveryInstructions(request.getDeliveryInstructions());
        load.setPalletCount(request.getPalletCount());
        load.setPieceCount(request.getPieceCount());
        load.setLengthInches(request.getLengthInches());
        load.setWidthInches(request.getWidthInches());
        load.setHeightInches(request.getHeightInches());
        load.setLiftgateRequired(request.isLiftgateRequired());
        load.setPalletJackRequired(request.isPalletJackRequired());
        load.setDockHighRequired(request.isDockHighRequired());
        load.setResidentialDelivery(request.isResidentialDelivery());
    }

    private void copyAdvancedFields(Load source, Load target) {
        target.setDescription(source.getDescription());
        target.setPickupStreetAddress(source.getPickupStreetAddress());
        target.setPickupZipCode(source.getPickupZipCode());
        target.setPickupLocationName(source.getPickupLocationName());
        target.setPickupContactName(source.getPickupContactName());
        target.setPickupContactPhone(source.getPickupContactPhone());
        target.setPickupTimeWindowStart(source.getPickupTimeWindowStart());
        target.setPickupTimeWindowEnd(source.getPickupTimeWindowEnd());
        target.setPickupInstructions(source.getPickupInstructions());
        target.setDeliveryStreetAddress(source.getDeliveryStreetAddress());
        target.setDeliveryZipCode(source.getDeliveryZipCode());
        target.setDeliveryLocationName(source.getDeliveryLocationName());
        target.setDeliveryContactName(source.getDeliveryContactName());
        target.setDeliveryContactPhone(source.getDeliveryContactPhone());
        target.setDeliveryTimeWindowStart(source.getDeliveryTimeWindowStart());
        target.setDeliveryTimeWindowEnd(source.getDeliveryTimeWindowEnd());
        target.setDeliveryInstructions(source.getDeliveryInstructions());
        target.setPalletCount(source.getPalletCount());
        target.setPieceCount(source.getPieceCount());
        target.setLengthInches(source.getLengthInches());
        target.setWidthInches(source.getWidthInches());
        target.setHeightInches(source.getHeightInches());
        target.setLiftgateRequired(source.isLiftgateRequired());
        target.setPalletJackRequired(source.isPalletJackRequired());
        target.setDockHighRequired(source.isDockHighRequired());
        target.setResidentialDelivery(source.isResidentialDelivery());
    }

    private void notifyFleetAboutCancellation(Load load, User actor) {
        offerRepository.findFirstByLoadIdAndStatus(load.getId(), OfferStatus.CONFIRMED)
                .or(() -> offerRepository.findFirstByLoadIdAndStatus(load.getId(), OfferStatus.SELECTED))
                .map(Offer::getFleetUser)
                .map(User::getCompany)
                .filter(company -> actor.getCompany() == null || !company.getId().equals(actor.getCompany().getId()))
                .ifPresent(company -> notificationService.createForCompany(
                        company,
                        NotificationType.LOAD_CANCELLED,
                        "Load cancelled",
                        "The broker cancelled a load tied to your offer",
                        "LOAD",
                        load.getId()
                ));
    }
}
