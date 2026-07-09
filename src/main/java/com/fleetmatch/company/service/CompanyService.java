package com.fleetmatch.company.service;

import com.fleetmatch.common.exception.BusinessRuleException;
import com.fleetmatch.common.exception.ResourceNotFoundException;
import com.fleetmatch.audit.entity.AuditAction;
import com.fleetmatch.audit.service.AuditLogService;
import com.fleetmatch.company.documents.entity.DocumentReviewStatus;
import com.fleetmatch.company.documents.entity.DocumentType;
import com.fleetmatch.company.documents.repository.CompanyDocumentRepository;
import com.fleetmatch.company.dto.CompanyProfileResponse;
import com.fleetmatch.company.dto.PendingCompanyResponse;
import com.fleetmatch.company.dto.UpdateCompanyProfileRequest;
import com.fleetmatch.company.dto.UpdateCompanySettingsRequest;
import com.fleetmatch.company.entity.Company;
import com.fleetmatch.company.entity.CompanyType;
import com.fleetmatch.company.entity.CompanyVerificationStatus;
import com.fleetmatch.company.repository.CompanyRepository;
import com.fleetmatch.company.review.entity.CompanyReviewAction;
import com.fleetmatch.company.review.service.CompanyReviewEventService;
import com.fleetmatch.email.service.EmailTemplateService;
import com.fleetmatch.security.user.CustomUserDetails;
import com.fleetmatch.user.entity.User;
import com.fleetmatch.user.entity.CompanyUserRole;
import com.fleetmatch.user.entity.UserStatus;
import com.fleetmatch.user.repository.UserRepository;
import com.fleetmatch.notification.event.NotificationType;
import com.fleetmatch.notification.inapp.service.NotificationService;
import com.fleetmatch.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fleetmatch.company.dto.CompanyResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final EmailTemplateService emailTemplateService;
    private final CompanyReviewEventService companyReviewEventService;
    private final CompanyDocumentRepository companyDocumentRepository;
    private final VehicleRepository vehicleRepository;

    public void verifyCompany(UUID companyId) {
        verifyCompany(companyId, null);
    }

    /**
     * Approves a company for controlled marketplace access and advances all
     * non-suspended company users to the approved account state.
     */
    @Transactional
    public void verifyCompany(UUID companyId, CustomUserDetails currentUser) {

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        ensureReadyForApproval(company);

        company.setVerificationStatus(
                CompanyVerificationStatus.APPROVED
        );
        company.setRejectionReason(null);
        company.setAdditionalDocumentsRequest(null);

        companyRepository.save(company);
        List<User> users = userRepository.findByCompanyId(company.getId());
        users
                .stream()
                .filter(user -> user.getStatus() != UserStatus.SUSPENDED)
                .forEach(user -> user.setStatus(UserStatus.APPROVED));
        userRepository.saveAll(users);
        notificationService.createForCompany(
                company,
                NotificationType.COMPANY_APPROVED,
                "Company approved",
                "Your company has been approved",
                "COMPANY",
                company.getId()
        );
        notificationService.createForCompany(
                company,
                NotificationType.SUBSCRIPTION_AVAILABLE,
                "Subscription available",
                "Your company has been approved. You can now choose a subscription plan.",
                "COMPANY",
                company.getId()
        );
        emailCompanyUsers(company, "verification_approved", Map.of(
                "companyName", company.getLegalName()
        ));
        auditLogService.log(getActor(currentUser), AuditAction.COMPANY_APPROVED, "COMPANY", company.getId(), "Company approved");
        companyReviewEventService.record(
                company,
                getActor(currentUser),
                CompanyReviewAction.APPROVED,
                null,
                null,
                company.getVerificationNotes()
        );
    }

    public void rejectCompany(UUID companyId) {
        rejectCompany(companyId, null);
    }

    @Transactional
    public void rejectCompany(UUID companyId, CustomUserDetails currentUser) {
        rejectCompany(companyId, null, null, currentUser);
    }

    /**
     * Rejects operational verification while preserving the admin reason and
     * notes that drive the user's resubmission path.
     */
    @Transactional
    public void rejectCompany(
            UUID companyId,
            String reason,
            String notes,
            CustomUserDetails currentUser
    ) {

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        company.setVerificationStatus(
                CompanyVerificationStatus.REJECTED
        );
        company.setRejectionReason(reason);
        company.setVerificationNotes(notes);

        companyRepository.save(company);
        List<User> users = userRepository.findByCompanyId(company.getId());
        users
                .stream()
                .filter(user -> user.getStatus() != UserStatus.SUSPENDED)
                .forEach(user -> user.setStatus(UserStatus.REJECTED));
        userRepository.saveAll(users);
        notificationService.createForCompany(
                company,
                NotificationType.COMPANY_REJECTED,
                "Company rejected",
                "Your company verification was rejected",
                "COMPANY",
                company.getId()
        );
        emailCompanyUsers(company, "verification_rejected", Map.of(
                "companyName", company.getLegalName(),
                "reason", reason == null ? "" : reason
        ));
        auditLogService.log(
                getActor(currentUser),
                AuditAction.COMPANY_REJECTED,
                "COMPANY",
                company.getId(),
                details("Company rejected", reason)
        );
        companyReviewEventService.record(
                company,
                getActor(currentUser),
                CompanyReviewAction.REJECTED,
                null,
                reason,
                notes
        );
    }

    @Transactional
    public void suspendCompany(
            UUID companyId,
            String notes,
            CustomUserDetails currentUser
    ) {

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        company.setVerificationStatus(CompanyVerificationStatus.SUSPENDED);
        company.setVerificationNotes(notes);

        companyRepository.save(company);
        notificationService.createForCompany(
                company,
                NotificationType.COMPANY_SUSPENDED,
                "Company suspended",
                "Your company has been suspended by EasyFleetMatch operations",
                "COMPANY",
                company.getId()
        );
        auditLogService.log(
                getActor(currentUser),
                AuditAction.COMPANY_SUSPENDED,
                "COMPANY",
                company.getId(),
                details("Company suspended", notes)
        );
        companyReviewEventService.record(
                company,
                getActor(currentUser),
                CompanyReviewAction.SUSPENDED,
                null,
                null,
                notes
        );
    }

    @Transactional
    public void reactivateCompany(
            UUID companyId,
            String notes,
            CustomUserDetails currentUser
    ) {

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        company.setVerificationStatus(CompanyVerificationStatus.APPROVED);
        company.setVerificationNotes(notes);

        companyRepository.save(company);
        notificationService.createForCompany(
                company,
                NotificationType.COMPANY_REACTIVATED,
                "Company reactivated",
                "Your company has been reactivated",
                "COMPANY",
                company.getId()
        );
        auditLogService.log(
                getActor(currentUser),
                AuditAction.COMPANY_REACTIVATED,
                "COMPANY",
                company.getId(),
                details("Company reactivated", notes)
        );
        companyReviewEventService.record(
                company,
                getActor(currentUser),
                CompanyReviewAction.REACTIVATED,
                null,
                null,
                notes
        );
    }

    @Transactional(readOnly = true)
    public List<PendingCompanyResponse> getPendingCompanies() {

        return companyRepository.findByVerificationStatusIn(List.of(
                        CompanyVerificationStatus.PENDING,
                        CompanyVerificationStatus.UNDER_REVIEW
                ))
                .stream()
                .map(company -> new PendingCompanyResponse(
                        company.getId(),
                        company.getLegalName(),
                        company.getType()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CompanyResponse> getAllCompanies() {

        return companyRepository.findAll()
                .stream()
                .map(company -> new CompanyResponse(
                        company.getId(),
                        company.getLegalName(),
                        company.getType(),
                        company.getVerificationStatus()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public CompanyResponse getCompany(UUID companyId) {

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        return new CompanyResponse(
                company.getId(),
                company.getLegalName(),
                company.getType(),
                company.getVerificationStatus()
        );
    }

    @Transactional(readOnly = true)
    public CompanyProfileResponse getProfile(
            CustomUserDetails currentUser
    ) {

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Company company = user.getCompany();

        return new CompanyProfileResponse(
                company.getId(),
                company.getLegalName(),
                company.getType(),
                company.getVerificationStatus(),
                company.getMcNumber(),
                company.getDotNumber(),
                company.getEntityType(),
                company.getEin(),
                company.getStateOfFormation(),
                company.getHeadquarters(),
                company.getNormalizedHeadquarters(),
                company.isHeadquartersAddressVerified(),
                company.getHeadquartersAddressVerificationStatus(),
                company.getHeadquartersLatitude(),
                company.getHeadquartersLongitude(),
                company.getPrimaryContact(),
                company.getAuthorityStatus(),
                company.getBrokerBondOrTrust(),
                company.getInsuranceCoverage(),
                company.getOperatingRegions(),
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

    @Transactional
    public CompanyProfileResponse updateProfile(
            CustomUserDetails currentUser,
            UpdateCompanyProfileRequest request
    ) {

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Company company = user.getCompany();

        if (user.getCompanyUserRole() != CompanyUserRole.OWNER) {
            throw new AccessDeniedException("Only owners can update company profile");
        }

        ensureOnboardingEditable(user, company);

        String mcNumber = normalizeAuthorityNumber(request.getMcNumber());
        if ((company.getType() == CompanyType.BROKER || company.getType() == CompanyType.FLEET) && (isBlank(mcNumber) || !mcNumber.matches("^MC-\\d{5,8}$"))) {
            throw new BusinessRuleException("MC authority number must use format MC-123456");
        }

        company.setMcNumber(mcNumber);
        company.setDotNumber(company.getType() == CompanyType.BROKER ? null : request.getDotNumber());
        company.setEntityType(request.getEntityType());
        company.setEin(request.getEin());
        company.setStateOfFormation(request.getStateOfFormation());
        if (!java.util.Objects.equals(request.getHeadquarters(), company.getHeadquarters())) {
            company.setHeadquartersAddressVerified(false);
            company.setHeadquartersAddressVerificationStatus("PENDING");
            company.setNormalizedHeadquarters(null);
            company.setHeadquartersLatitude(null);
            company.setHeadquartersLongitude(null);
        }
        company.setHeadquarters(request.getHeadquarters());
        company.setPrimaryContact(request.getPrimaryContact());
        company.setAuthorityStatus(request.getAuthorityStatus());
        company.setBrokerBondOrTrust(request.getBrokerBondOrTrust());
        company.setInsuranceCoverage(request.getInsuranceCoverage());
        company.setOperatingRegions(request.getOperatingRegions());
        company.setPhone(user.isPhoneVerified() && !isBlank(user.getPhone()) ? user.getPhone() : request.getPhone());
        company.setWebsite(request.getWebsite());
        company.setDbaName(request.getDbaName());
        company.setEmail(request.getEmail());
        company.setFleetSize(request.getFleetSize());
        company.setDescription(request.getDescription());
        company.setCompanyInformationCompleted(true);

        if (user.getStatus() == UserStatus.PHONE_VERIFIED ||
                user.getStatus() == UserStatus.EMAIL_VERIFIED ||
                user.getStatus() == UserStatus.REGISTERED ||
                user.getStatus() == UserStatus.PENDING_VERIFICATION) {
            user.setStatus(UserStatus.DOCUMENTS_PENDING);
        }

        Company saved = companyRepository.save(company);
        userRepository.save(user);
        auditLogService.log(user, AuditAction.COMPANY_UPDATED, "COMPANY", saved.getId(), "Company profile updated");

        return new CompanyProfileResponse(
                saved.getId(),
                saved.getLegalName(),
                saved.getType(),
                saved.getVerificationStatus(),
                saved.getMcNumber(),
                saved.getDotNumber(),
                saved.getEntityType(),
                saved.getEin(),
                saved.getStateOfFormation(),
                saved.getHeadquarters(),
                saved.getNormalizedHeadquarters(),
                saved.isHeadquartersAddressVerified(),
                saved.getHeadquartersAddressVerificationStatus(),
                saved.getHeadquartersLatitude(),
                saved.getHeadquartersLongitude(),
                saved.getPrimaryContact(),
                saved.getAuthorityStatus(),
                saved.getBrokerBondOrTrust(),
                saved.getInsuranceCoverage(),
                saved.getOperatingRegions(),
                saved.getPhone(),
                saved.getWebsite(),
                saved.getDbaName(),
                saved.getEmail(),
                saved.getFleetSize(),
                saved.getDescription(),
                saved.isCompanyInformationCompleted(),
                saved.isMarketSurveyCompleted()
        );
    }

    @Transactional(readOnly = true)
    public CompanyProfileResponse getMyCompany(CustomUserDetails currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return toCompanyProfile(user.getCompany());
    }

    @Transactional
    public CompanyProfileResponse updateSettings(
            CustomUserDetails currentUser,
            UpdateCompanySettingsRequest request
    ) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getCompanyUserRole() != CompanyUserRole.OWNER) {
            throw new AccessDeniedException("Only owners can update company settings");
        }

        Company company = user.getCompany();

        ensureOnboardingEditable(user, company);

        company.setDbaName(request.getDbaName());
        company.setWebsite(request.getWebsite());
        company.setEmail(request.getCompanyEmail());
        company.setPhone(request.getCompanyPhone());
        company.setFleetSize(request.getFleetSize());
        company.setDescription(request.getDescription());
        company.setCompanyInformationCompleted(true);

        if (user.getStatus() == UserStatus.PHONE_VERIFIED ||
                user.getStatus() == UserStatus.EMAIL_VERIFIED ||
                user.getStatus() == UserStatus.REGISTERED ||
                user.getStatus() == UserStatus.PENDING_VERIFICATION) {
            user.setStatus(UserStatus.DOCUMENTS_PENDING);
        }

        Company saved = companyRepository.save(company);
        userRepository.save(user);
        auditLogService.log(user, AuditAction.COMPANY_UPDATED, "COMPANY", saved.getId(), "Company settings updated");
        return toCompanyProfile(saved);
    }

    private void ensureOnboardingEditable(User user, Company company) {
        if (user.getStatus() == UserStatus.IN_REVIEW ||
                company.getVerificationStatus() == CompanyVerificationStatus.UNDER_REVIEW) {
            throw new BusinessRuleException(
                    "Company onboarding is locked during admin review. Contact support for urgent corrections."
            );
        }
    }

    private CompanyProfileResponse toCompanyProfile(Company company) {
        return new CompanyProfileResponse(
                company.getId(),
                company.getLegalName(),
                company.getType(),
                company.getVerificationStatus(),
                company.getMcNumber(),
                company.getDotNumber(),
                company.getEntityType(),
                company.getEin(),
                company.getStateOfFormation(),
                company.getHeadquarters(),
                company.getNormalizedHeadquarters(),
                company.isHeadquartersAddressVerified(),
                company.getHeadquartersAddressVerificationStatus(),
                company.getHeadquartersLatitude(),
                company.getHeadquartersLongitude(),
                company.getPrimaryContact(),
                company.getAuthorityStatus(),
                company.getBrokerBondOrTrust(),
                company.getInsuranceCoverage(),
                company.getOperatingRegions(),
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

    private User getActor(CustomUserDetails currentUser) {
        if (currentUser == null) {
            return null;
        }

        return userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private void ensureReadyForApproval(Company company) {
        if (company.getVerificationStatus() != CompanyVerificationStatus.UNDER_REVIEW) {
            throw new BusinessRuleException("Company must be in review before approval");
        }

        if (!company.isCompanyInformationCompleted()) {
            throw new BusinessRuleException("Company information must be completed before approval");
        }

        if (!company.isMarketSurveyCompleted()) {
            throw new BusinessRuleException("Market survey must be completed before approval");
        }

        if (company.getType() == CompanyType.FLEET &&
                vehicleRepository.countByCompanyIdAndActiveTrue(company.getId()) == 0) {
            throw new BusinessRuleException("Fleet vehicle information must be completed before approval");
        }

        List<DocumentType> missingApprovedDocuments = requiredDocuments(company.getType())
                .stream()
                .filter(documentType -> !companyDocumentRepository.existsByCompanyIdAndDocumentTypeAndReviewStatus(
                        company.getId(),
                        documentType,
                        DocumentReviewStatus.APPROVED
                ))
                .toList();

        if (!missingApprovedDocuments.isEmpty()) {
            throw new BusinessRuleException(
                    "All required documents must be approved before company approval: " +
                            missingApprovedDocuments
            );
        }
    }

    private List<DocumentType> requiredDocuments(CompanyType type) {
        if (type == CompanyType.BROKER) {
            return List.of(
                    DocumentType.BUSINESS_REGISTRATION,
                    DocumentType.CERTIFICATE_OF_INSURANCE,
                    DocumentType.MC_AUTHORITY
            );
        }

        return List.of(
                DocumentType.DOT_REGISTRATION,
                DocumentType.MC_AUTHORITY,
                DocumentType.CERTIFICATE_OF_INSURANCE,
                DocumentType.BUSINESS_REGISTRATION
        );
    }

    private String normalizeAuthorityNumber(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        return value.trim().toUpperCase();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String details(String action, String notes) {
        if (notes == null || notes.isBlank()) {
            return action;
        }

        return action + ": " + notes;
    }

    private void emailCompanyUsers(
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
