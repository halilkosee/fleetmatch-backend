package com.fleetmatch.admin.service;

import com.fleetmatch.admin.dto.AdminApprovalQueueItemResponse;
import com.fleetmatch.admin.dto.AdminCompanyReviewResponse;
import com.fleetmatch.admin.dto.AdminConversationResponse;
import com.fleetmatch.admin.dto.AdminLoadResponse;
import com.fleetmatch.admin.dto.AdminOfferResponse;
import com.fleetmatch.admin.dto.AdminReviewActionRequest;
import com.fleetmatch.admin.dto.PendingUserResponse;
import com.fleetmatch.audit.entity.AuditAction;
import com.fleetmatch.audit.service.AuditLogService;
import com.fleetmatch.common.exception.BusinessRuleException;
import com.fleetmatch.company.document.dto.CompanyDocumentResponse;
import com.fleetmatch.company.document.dto.ReviewCompanyDocumentRequest;
import com.fleetmatch.company.document.entity.CompanyDocument;
import com.fleetmatch.company.document.entity.DocumentReviewStatus;
import com.fleetmatch.company.document.repository.CompanyDocumentRepository;
import com.fleetmatch.company.dto.CompanyProfileResponse;
import com.fleetmatch.company.entity.Company;
import com.fleetmatch.company.entity.CompanyVerificationStatus;
import com.fleetmatch.company.review.entity.CompanyReviewAction;
import com.fleetmatch.company.review.service.CompanyReviewEventService;
import com.fleetmatch.load.dto.LoadResponse;
import com.fleetmatch.load.entity.Load;
import com.fleetmatch.messaging.entity.Conversation;
import com.fleetmatch.messaging.dto.MessageResponse;
import com.fleetmatch.messaging.entity.Message;
import com.fleetmatch.messaging.repository.ConversationRepository;
import com.fleetmatch.messaging.repository.MessageRepository;
import com.fleetmatch.messaging.service.MessagingService;
import com.fleetmatch.notification.entity.NotificationType;
import com.fleetmatch.notification.service.NotificationService;
import com.fleetmatch.offer.entity.Offer;
import com.fleetmatch.onboarding.dto.OnboardingAnalyticsResponse;
import com.fleetmatch.onboarding.entity.MarketSurvey;
import com.fleetmatch.onboarding.repository.MarketSurveyRepository;
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
import com.fleetmatch.email.service.EmailTemplateService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;

    private final CompanyRepository companyRepository;
    private final LoadRepository loadRepository;
    private final OfferRepository offerRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final CompanyDocumentRepository companyDocumentRepository;
    private final MarketSurveyRepository marketSurveyRepository;
    private final CompanyReviewEventService companyReviewEventService;
    private final LoadService loadService;
    private final MessagingService messagingService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final EmailTemplateService emailTemplateService;

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

    @Transactional(readOnly = true)
    public Page<AdminApprovalQueueItemResponse> getApprovalQueue(
            CompanyType companyType,
            Pageable pageable
    ) {
        List<CompanyVerificationStatus> statuses = List.of(
                CompanyVerificationStatus.PENDING,
                CompanyVerificationStatus.UNDER_REVIEW,
                CompanyVerificationStatus.REJECTED
        );

        Page<Company> companies = companyType == null
                ? companyRepository.findByVerificationStatusIn(statuses, pageable)
                : companyRepository.findByVerificationStatusInAndType(statuses, companyType, pageable);

        return companies.map(this::toApprovalQueueItem);
    }

    @Transactional(readOnly = true)
    public AdminCompanyReviewResponse getCompanyReview(UUID companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        List<CompanyDocumentResponse> documents =
                companyDocumentRepository.findByCompanyId(company.getId())
                        .stream()
                        .map(document -> new CompanyDocumentResponse(
                                document.getId(),
                                document.getDocumentType(),
                                document.getFileName(),
                                document.getFileUrl(),
                                document.getOriginalFileName(),
                                document.getContentType(),
                                document.getFileSizeBytes(),
                                document.getReviewStatus(),
                                document.getReviewNotes(),
                                document.getReviewedAt(),
                                document.getUploadedAt()
                        ))
                        .toList();

        AdminCompanyReviewResponse.Survey survey =
                marketSurveyRepository.findByCompanyId(company.getId())
                        .map(this::toReviewSurvey)
                        .orElse(null);

        return new AdminCompanyReviewResponse(
                company.getId(),
                toCompanyProfile(company),
                company.getVerificationStatus(),
                company.getVerificationNotes(),
                company.getRejectionReason(),
                company.getAdditionalDocumentsRequest(),
                company.getAdminInternalNotes(),
                company.getManualPriority(),
                company.getCreatedAt(),
                documents,
                companyReviewEventService.getEvents(company.getId()),
                survey
        );
    }

    @Transactional
    public CompanyDocumentResponse reviewCompanyDocument(
            UUID companyId,
            UUID documentId,
            ReviewCompanyDocumentRequest request,
            CustomUserDetails currentUser
    ) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        CompanyDocument document = companyDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Company document not found"));

        if (!document.getCompany().getId().equals(company.getId())) {
            throw new BusinessRuleException("Document does not belong to this company");
        }

        document.setReviewStatus(request.getReviewStatus());
        document.setReviewNotes(request.getReviewNotes());
        document.setReviewedAt(java.time.LocalDateTime.now());
        document.setReviewedByUserId(currentUser.getId());

        CompanyDocument saved = companyDocumentRepository.save(document);
        companyReviewEventService.record(
                company,
                getActor(currentUser),
                CompanyReviewAction.DOCUMENT_REVIEWED,
                saved.getId(),
                saved.getReviewStatus().name(),
                saved.getReviewNotes()
        );

        if (saved.getReviewStatus() == DocumentReviewStatus.REJECTED ||
                saved.getReviewStatus() == DocumentReviewStatus.REQUESTED_AGAIN) {
            company.setVerificationStatus(CompanyVerificationStatus.PENDING);
            company.setAdditionalDocumentsRequest(saved.getReviewNotes());
            companyRepository.save(company);
        }

        return new CompanyDocumentResponse(
                saved.getId(),
                saved.getDocumentType(),
                saved.getFileName(),
                saved.getFileUrl(),
                saved.getOriginalFileName(),
                saved.getContentType(),
                saved.getFileSizeBytes(),
                saved.getReviewStatus(),
                saved.getReviewNotes(),
                saved.getReviewedAt(),
                saved.getUploadedAt()
        );
    }

    @Transactional
    public void requestAdditionalDocuments(
            UUID companyId,
            AdminReviewActionRequest request,
            CustomUserDetails currentUser
    ) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        if (request.getReason() == null || request.getReason().isBlank()) {
            throw new BusinessRuleException("Reason is required");
        }

        company.setVerificationStatus(CompanyVerificationStatus.PENDING);
        company.setAdditionalDocumentsRequest(request.getReason());
        company.setVerificationNotes(request.getNotes());
        company.setManualPriority(request.getManualPriority());
        Company saved = companyRepository.save(company);

        notificationService.createForCompany(
                saved,
                NotificationType.ADDITIONAL_DOCUMENTS_REQUESTED,
                "Additional documents requested",
                "EasyFleetMatch operations requested additional verification documents",
                "COMPANY",
                saved.getId()
        );
        emailCompanyOwners(saved, "additional_documents_requested", Map.of(
                "companyName", saved.getLegalName(),
                "reason", request.getReason()
        ));
        auditLogService.log(
                getActor(currentUser),
                AuditAction.COMPANY_ADDITIONAL_DOCUMENTS_REQUESTED,
                "COMPANY",
                saved.getId(),
                "Additional documents requested: " + request.getReason()
        );
        companyReviewEventService.record(
                saved,
                getActor(currentUser),
                CompanyReviewAction.ADDITIONAL_DOCUMENTS_REQUESTED,
                null,
                request.getReason(),
                request.getNotes()
        );
    }

    @Transactional
    public void addInternalNote(
            UUID companyId,
            AdminReviewActionRequest request,
            CustomUserDetails currentUser
    ) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        company.setAdminInternalNotes(request.getNotes());
        company.setManualPriority(request.getManualPriority());
        Company saved = companyRepository.save(company);
        auditLogService.log(
                getActor(currentUser),
                AuditAction.COMPANY_INTERNAL_NOTE_ADDED,
                "COMPANY",
                saved.getId(),
                "Internal review note updated"
        );
        companyReviewEventService.record(
                saved,
                getActor(currentUser),
                CompanyReviewAction.INTERNAL_NOTE_UPDATED,
                null,
                null,
                request.getNotes()
        );
    }

    @Transactional(readOnly = true)
    public Page<AdminOfferResponse> getOffers(
            OfferStatus status,
            Pageable pageable
    ) {
        Page<Offer> offers = status == null
                ? offerRepository.findAll(pageable)
                : offerRepository.findByStatus(status, pageable);

        return offers.map(this::toAdminOfferResponse);
    }

    @Transactional(readOnly = true)
    public AdminOfferResponse getOffer(UUID offerId) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found"));
        return toAdminOfferResponse(offer);
    }

    @Transactional
    public AdminOfferResponse cancelOffer(
            UUID offerId,
            AdminReviewActionRequest request,
            CustomUserDetails currentUser
    ) {
        Offer offer = offerRepository.findByIdWithLoadForUpdate(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found"));

        if (offer.getStatus() == OfferStatus.CONFIRMED ||
                offer.getStatus() == OfferStatus.REJECTED ||
                offer.getStatus() == OfferStatus.WITHDRAWN) {
            throw new BusinessRuleException("Offer cannot be cancelled from its current status");
        }

        Load load = offer.getLoad();
        if (offer.getStatus() == OfferStatus.SELECTED) {
            load.setStatus(LoadStatus.POSTED);
            loadRepository.save(load);
            messagingService.archiveConversation(load.getId());
        }

        offer.setStatus(OfferStatus.WITHDRAWN);
        Offer saved = offerRepository.save(offer);
        notificationService.createForCompany(
                saved.getFleetUser().getCompany(),
                NotificationType.OFFER_REJECTED,
                "Offer cancelled",
                "EasyFleetMatch operations cancelled an offer",
                "OFFER",
                saved.getId()
        );
        auditLogService.log(
                getActor(currentUser),
                AuditAction.OFFER_CANCELLED,
                "OFFER",
                saved.getId(),
                request == null || request.getReason() == null
                        ? "Offer cancelled by admin"
                        : "Offer cancelled by admin: " + request.getReason()
        );

        return toAdminOfferResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<AdminConversationResponse> getConversations(
            UUID companyId,
            boolean includeArchived,
            Pageable pageable
    ) {
        Page<Conversation> conversations;
        if (companyId != null) {
            conversations = conversationRepository.findByBrokerCompanyIdOrFleetCompanyId(
                    companyId,
                    companyId,
                    pageable
            );
        } else if (includeArchived) {
            conversations = conversationRepository.findAll(pageable);
        } else {
            conversations = conversationRepository.findByArchivedAtIsNull(pageable);
        }

        return conversations.map(this::toAdminConversationResponse);
    }

    @Transactional(readOnly = true)
    public AdminConversationResponse getConversation(UUID conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));
        return toAdminConversationResponse(conversation);
    }

    @Transactional(readOnly = true)
    public Page<MessageResponse> getConversationMessages(
            UUID conversationId,
            Pageable pageable
    ) {
        conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        return messageRepository.findByConversationIdAndDeletedAtIsNullOrderByCreatedAtAsc(
                conversationId,
                pageable
        ).map(this::toMessageResponse);
    }

    @Transactional(readOnly = true)
    public OnboardingAnalyticsResponse getOnboardingAnalytics() {
        return new OnboardingAnalyticsResponse(
                companyRepository.countByType(CompanyType.BROKER),
                companyRepository.countByType(CompanyType.FLEET),
                companyRepository.countByVerificationStatusIn(List.of(
                        CompanyVerificationStatus.PENDING,
                        CompanyVerificationStatus.UNDER_REVIEW
                )),
                companyRepository.countByVerificationStatus(CompanyVerificationStatus.APPROVED),
                companyRepository.countByVerificationStatus(CompanyVerificationStatus.REJECTED),
                topListMetric(MarketSurvey::getOperatingStates),
                topListMetric(MarketSurvey::getEquipmentTypes),
                topStringMetric(MarketSurvey::getCurrentLoadBoard),
                topStringMetric(MarketSurvey::getCurrentTms)
        );
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

    private AdminApprovalQueueItemResponse toApprovalQueueItem(Company company) {
        MarketSurvey survey = marketSurveyRepository.findByCompanyId(company.getId())
                .orElse(null);

        return new AdminApprovalQueueItemResponse(
                company.getId(),
                company.getLegalName(),
                company.getType(),
                company.getVerificationStatus(),
                company.getFleetSize(),
                survey == null ? List.of() : nullToEmpty(survey.getOperatingStates()),
                survey == null ? List.of() : nullToEmpty(survey.getEquipmentTypes()),
                survey == null ? null : survey.getAverageLoadsPerWeek(),
                company.getManualPriority(),
                company.getCreatedAt(),
                companyDocumentRepository.existsByCompanyId(company.getId()),
                company.isMarketSurveyCompleted(),
                company.getVerificationNotes()
        );
    }

    private AdminCompanyReviewResponse.Survey toReviewSurvey(MarketSurvey survey) {
        return new AdminCompanyReviewResponse.Survey(
                survey.getCompanyType(),
                nullToEmpty(survey.getOperatingStates()),
                nullToEmpty(survey.getEquipmentTypes()),
                survey.getAverageLoadsPerWeek(),
                survey.getFleetSize(),
                survey.getCurrentLoadBoard(),
                survey.getCurrentTms(),
                survey.getFutureIntegrationInterest(),
                survey.getBiggestOperationalChallenges(),
                survey.getHomeState(),
                nullToEmpty(survey.getPreferredRegions()),
                survey.getPreferredMileage(),
                survey.getDedicatedRouteInterest()
        );
    }

    private CompanyProfileResponse toCompanyProfile(Company company) {
        return new CompanyProfileResponse(
                company.getId(),
                company.getLegalName(),
                company.getType(),
                company.getVerificationStatus(),
                company.getMcNumber(),
                company.getDotNumber(),
                company.getPhone(),
                company.getWebsite(),
                company.getDbaName(),
                company.getEmail(),
                company.getFleetSize(),
                company.getDescription(),
                company.isCompanyInformationCompleted(),
                company.isMarketSurveyCompleted()
        );
    }

    private AdminOfferResponse toAdminOfferResponse(Offer offer) {
        User fleetUser = offer.getFleetUser();
        return new AdminOfferResponse(
                offer.getId(),
                offer.getLoad().getId(),
                offer.getStatus(),
                offer.getAmount(),
                offer.getMessage(),
                fleetUser.getCompany().getId(),
                fleetUser.getCompany().getLegalName(),
                fleetUser.getId(),
                fleetUser.getFirstName() + " " + fleetUser.getLastName(),
                fleetUser.getEmail(),
                offer.getLoad().getBrokerCompany().getId(),
                offer.getLoad().getBrokerCompany().getLegalName(),
                offer.getCreatedAt(),
                offer.getUpdatedAt()
        );
    }

    private AdminConversationResponse toAdminConversationResponse(Conversation conversation) {
        return new AdminConversationResponse(
                conversation.getId(),
                conversation.getLoad().getId(),
                conversation.getBrokerCompany().getId(),
                conversation.getBrokerCompany().getLegalName(),
                conversation.getFleetCompany().getId(),
                conversation.getFleetCompany().getLegalName(),
                conversation.isArchived(),
                conversation.getArchivedAt(),
                messageRepository.countByConversationId(conversation.getId()),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }

    private MessageResponse toMessageResponse(Message message) {
        User sender = message.getSenderUser();
        Company senderCompany = message.getSenderCompany();

        return new MessageResponse(
                message.getId(),
                message.getConversation().getId(),
                sender.getId(),
                sender.getFirstName() + " " + sender.getLastName(),
                senderCompany.getId(),
                senderCompany.getLegalName(),
                message.isDeleted() ? null : message.getBody(),
                message.isRead(),
                message.getReadAt(),
                message.isDeleted(),
                message.getDeletedAt(),
                message.getCreatedAt(),
                message.getUpdatedAt()
        );
    }

    private List<OnboardingAnalyticsResponse.Metric> topListMetric(
            java.util.function.Function<MarketSurvey, List<String>> extractor
    ) {
        Map<String, Long> counts = new LinkedHashMap<>();
        marketSurveyRepository.findAll()
                .stream()
                .map(extractor)
                .filter(values -> values != null)
                .flatMap(List::stream)
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .forEach(value -> counts.merge(value, 1L, Long::sum));

        return topMetrics(counts);
    }

    private List<OnboardingAnalyticsResponse.Metric> topStringMetric(
            java.util.function.Function<MarketSurvey, String> extractor
    ) {
        Map<String, Long> counts = new LinkedHashMap<>();
        marketSurveyRepository.findAll()
                .stream()
                .map(extractor)
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .forEach(value -> counts.merge(value, 1L, Long::sum));

        return topMetrics(counts);
    }

    private List<OnboardingAnalyticsResponse.Metric> topMetrics(Map<String, Long> counts) {
        return counts.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(10)
                .map(entry -> new OnboardingAnalyticsResponse.Metric(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<String> nullToEmpty(List<String> values) {
        return values == null ? List.of() : values;
    }

    private void emailCompanyOwners(
            Company company,
            String templateKey,
            Map<String, String> variables
    ) {
        userRepository.findByCompanyId(company.getId())
                .forEach(user -> emailTemplateService.sendTemplate(
                        templateKey,
                        user.getEmail(),
                        variables
                ));
    }
}
