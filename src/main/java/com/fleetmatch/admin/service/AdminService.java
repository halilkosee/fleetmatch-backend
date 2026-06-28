package com.fleetmatch.admin.service;

import com.fleetmatch.admin.dto.AdminLoadResponse;
import com.fleetmatch.admin.dto.PendingUserResponse;
import com.fleetmatch.audit.entity.AuditAction;
import com.fleetmatch.audit.service.AuditLogService;
import com.fleetmatch.load.dto.LoadResponse;
import com.fleetmatch.load.entity.Load;
import com.fleetmatch.security.user.CustomUserDetails;
import java.util.List;
import com.fleetmatch.common.exception.ResourceNotFoundException;
import com.fleetmatch.user.entity.User;
import com.fleetmatch.user.entity.UserStatus;
import com.fleetmatch.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fleetmatch.admin.dto.AdminDashboardResponse;
import com.fleetmatch.company.entity.CompanyType;
import com.fleetmatch.company.repository.CompanyRepository;
import com.fleetmatch.load.entity.LoadStatus;
import com.fleetmatch.load.repository.LoadRepository;
import com.fleetmatch.load.service.LoadService;
import com.fleetmatch.offer.entity.OfferStatus;
import com.fleetmatch.offer.repository.OfferRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;

    private final CompanyRepository companyRepository;
    private final LoadRepository loadRepository;
    private final OfferRepository offerRepository;
    private final LoadService loadService;
    private final AuditLogService auditLogService;

    public void approveUser(UUID userId, CustomUserDetails currentUser) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        user.setStatus(UserStatus.ACTIVE);

        User saved = userRepository.save(user);
        auditLogService.log(
                getActor(currentUser),
                AuditAction.USER_APPROVED,
                "USER",
                saved.getId(),
                "User approved"
        );
    }

    public void suspendUser(UUID userId, CustomUserDetails currentUser) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        user.setStatus(UserStatus.SUSPENDED);

        User saved = userRepository.save(user);
        auditLogService.log(
                getActor(currentUser),
                AuditAction.USER_SUSPENDED,
                "USER",
                saved.getId(),
                "User suspended"
        );
    }

    public void unlockUser(UUID userId, CustomUserDetails currentUser) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);

        User saved = userRepository.save(user);
        auditLogService.log(
                getActor(currentUser),
                AuditAction.USER_UNLOCKED,
                "USER",
                saved.getId(),
                "User account unlocked"
        );
    }

    public List<PendingUserResponse> getPendingUsers() {

        return userRepository.findByStatusIn(List.of(
                        UserStatus.REGISTERED,
                        UserStatus.EMAIL_VERIFIED,
                        UserStatus.PHONE_VERIFIED,
                        UserStatus.DOCUMENTS_PENDING,
                        UserStatus.IN_REVIEW,
                        UserStatus.REJECTED,
                        UserStatus.PENDING_VERIFICATION
                ))
                .stream()
                .map(user -> new PendingUserResponse(
                        user.getId(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getEmail(),
                        user.getPlatformRole(),
                        user.getStatus(),
                        user.getCompany() != null
                                ? user.getCompany().getLegalName()
                                : null
                ))
                .toList();
    }

    public AdminDashboardResponse getDashboard() {

        var users = new AdminDashboardResponse.UserStats(
                onboardingUserCount(),
                userRepository.countByStatus(UserStatus.ACTIVE),
                userRepository.countByStatus(UserStatus.SUSPENDED)
        );

        var companies = new AdminDashboardResponse.CompanyStats(
                companyRepository.countByType(CompanyType.BROKER),
                companyRepository.countByType(CompanyType.FLEET)
        );

        var loads = new AdminDashboardResponse.LoadStats(
                loadRepository.countByStatus(LoadStatus.POSTED),
                loadRepository.countByStatus(LoadStatus.AWAITING_FLEET_CONFIRMATION),
                loadRepository.countByStatus(LoadStatus.BOOKED),
                loadRepository.countByStatus(LoadStatus.IN_TRANSIT),
                loadRepository.countByStatus(LoadStatus.DELIVERED),
                loadRepository.countByStatus(LoadStatus.CANCELLED)
        );

        var offers = new AdminDashboardResponse.OfferStats(
                offerRepository.countByStatus(OfferStatus.PENDING),
                offerRepository.countByStatus(OfferStatus.SELECTED),
                offerRepository.countByStatus(OfferStatus.CONFIRMED),
                offerRepository.countByStatus(OfferStatus.REJECTED),
                offerRepository.countByStatus(OfferStatus.WITHDRAWN)
        );

        return new AdminDashboardResponse(
                users,
                companies,
                loads,
                offers
        );
    }

    @Transactional(readOnly = true)
    public Page<AdminLoadResponse> getLoads(
            LoadStatus status,
            UUID brokerCompanyId,
            String keyword,
            Pageable pageable
    ) {
        return loadRepository.findAll(
                adminLoadSpecification(status, brokerCompanyId, keyword),
                pageable
        ).map(this::toAdminLoadResponse);
    }

    @Transactional(readOnly = true)
    public AdminLoadResponse getLoad(UUID loadId) {
        Load load = loadRepository.findById(loadId)
                .orElseThrow(() -> new ResourceNotFoundException("Load not found"));

        return toAdminLoadResponse(load);
    }

    @Transactional
    public LoadResponse cancelLoad(
            UUID loadId,
            CustomUserDetails currentUser
    ) {
        return loadService.cancelLoad(loadId, currentUser);
    }

    private User getActor(CustomUserDetails currentUser) {
        return userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));
    }

    private long onboardingUserCount() {
        return List.of(
                        UserStatus.REGISTERED,
                        UserStatus.EMAIL_VERIFIED,
                        UserStatus.PHONE_VERIFIED,
                        UserStatus.DOCUMENTS_PENDING,
                        UserStatus.IN_REVIEW,
                        UserStatus.REJECTED,
                        UserStatus.PENDING_VERIFICATION
                )
                .stream()
                .mapToLong(userRepository::countByStatus)
                .sum();
    }

    private Specification<Load> adminLoadSpecification(
            LoadStatus status,
            UUID brokerCompanyId,
            String keyword
    ) {
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (brokerCompanyId != null) {
                predicates.add(cb.equal(root.get("brokerCompany").get("id"), brokerCompanyId));
            }

            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("commodity")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern),
                        cb.like(cb.lower(root.get("referenceNumber")), pattern),
                        cb.like(cb.lower(root.get("pickupCity")), pattern),
                        cb.like(cb.lower(root.get("deliveryCity")), pattern)
                ));
            }

            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private AdminLoadResponse toAdminLoadResponse(Load load) {
        User createdBy = load.getCreatedBy();

        return new AdminLoadResponse(
                load.getId(),
                load.getStatus(),
                load.getBrokerCompany().getId(),
                load.getBrokerCompany().getLegalName(),
                load.getBrokerCompany().getEmail(),
                load.getBrokerCompany().getPhone(),
                createdBy.getId(),
                createdBy.getFirstName() + " " + createdBy.getLastName(),
                createdBy.getEmail(),
                offerRepository.countByLoadId(load.getId()),
                load.getPickupCity(),
                load.getPickupState(),
                load.getPickupDate(),
                load.getDeliveryCity(),
                load.getDeliveryState(),
                load.getDeliveryDate(),
                load.getEquipmentType(),
                load.getWeight(),
                load.getWeightLbs(),
                load.getRate(),
                load.getMiles(),
                load.getCommodity(),
                load.getReferenceNumber(),
                load.getNotes(),
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
                load.isResidentialDelivery(),
                load.getCreatedAt(),
                load.getUpdatedAt()
        );
    }
}
