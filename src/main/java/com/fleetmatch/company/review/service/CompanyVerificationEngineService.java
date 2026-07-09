package com.fleetmatch.company.review.service;

import com.fleetmatch.common.exception.BusinessRuleException;
import com.fleetmatch.common.exception.ResourceNotFoundException;
import com.fleetmatch.company.entity.Company;
import com.fleetmatch.company.entity.CompanyType;
import com.fleetmatch.company.repository.CompanyRepository;
import com.fleetmatch.company.review.dto.UpdateVerificationChecklistItemRequest;
import com.fleetmatch.company.review.dto.UpdateVerificationSectionReviewRequest;
import com.fleetmatch.company.review.dto.VerificationChecklistItemResponse;
import com.fleetmatch.company.review.dto.VerificationRiskResponse;
import com.fleetmatch.company.review.dto.VerificationSectionReviewResponse;
import com.fleetmatch.company.review.dto.VerificationSnapshotResponse;
import com.fleetmatch.company.review.entity.CompanyReviewAction;
import com.fleetmatch.company.review.entity.CompanyVerificationChecklistItem;
import com.fleetmatch.company.review.entity.CompanyVerificationRiskAssessment;
import com.fleetmatch.company.review.entity.CompanyVerificationSectionReview;
import com.fleetmatch.company.review.entity.CompanyVerificationSnapshot;
import com.fleetmatch.company.review.entity.SectionReviewStatus;
import com.fleetmatch.company.review.entity.VerificationRiskLevel;
import com.fleetmatch.company.review.repository.CompanyVerificationChecklistItemRepository;
import com.fleetmatch.company.review.repository.CompanyVerificationRiskAssessmentRepository;
import com.fleetmatch.company.review.repository.CompanyVerificationSectionReviewRepository;
import com.fleetmatch.company.review.repository.CompanyVerificationSnapshotRepository;
import com.fleetmatch.onboarding.dto.OnboardingValidationResponse;
import com.fleetmatch.user.entity.User;
import com.fleetmatch.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyVerificationEngineService {

    private final CompanyVerificationSnapshotRepository snapshotRepository;
    private final CompanyVerificationRiskAssessmentRepository riskAssessmentRepository;
    private final CompanyVerificationChecklistItemRepository checklistItemRepository;
    private final CompanyVerificationSectionReviewRepository sectionReviewRepository;
    private final CompanyRepository companyRepository;
    private final VehicleRepository vehicleRepository;
    private final CompanyReviewEventService companyReviewEventService;

    @Transactional
    public CompanyVerificationSnapshot createSnapshot(
            Company company,
            User submittedBy,
            OnboardingValidationResponse validation
    ) {
        int nextVersion = snapshotRepository
                .findTopByCompanyIdOrderByVersionNumberDesc(company.getId())
                .map(snapshot -> snapshot.getVersionNumber() + 1)
                .orElse(1);

        CompanyVerificationSnapshot snapshot = new CompanyVerificationSnapshot();
        snapshot.setCompany(company);
        snapshot.setSubmittedByUser(submittedBy);
        snapshot.setVersionNumber(nextVersion);
        snapshot.setCompanyType(company.getType());
        snapshot.setUserStatus(submittedBy.getStatus());
        snapshot.setCompanyVerificationStatus(company.getVerificationStatus());
        snapshot.setSubmittedAt(LocalDateTime.now());
        snapshot.setCompletionPercentage(validation.getCompletionPercentage());
        snapshot.setSubmissionReady(validation.isSubmissionReady());
        snapshot.setCompletedSections(validation.getCompletedSections());
        snapshot.setIncompleteSections(validation.getIncompleteSections());
        snapshot.setMissingFields(validation.getMissingFields());
        snapshot.setMissingDocuments(validation.getMissingDocuments().stream().map(Enum::name).toList());
        snapshot.setInvalidFields(validation.getInvalidFields());
        snapshot.setWarnings(validation.getWarnings());
        snapshot.setBlockingErrors(validation.getBlockingErrors());
        snapshot.setLegalName(company.getLegalName());
        snapshot.setEmail(company.getEmail());
        snapshot.setPhone(company.getPhone());
        snapshot.setMcNumber(company.getMcNumber());
        snapshot.setDotNumber(company.getDotNumber());
        snapshot.setEin(company.getEin());
        snapshot.setHeadquarters(company.getHeadquarters());
        snapshot.setWebsite(company.getWebsite());
        snapshot.setFleetSize(company.getFleetSize());
        snapshot.setActiveVehicleCount(vehicleRepository.countByCompanyIdAndActiveTrue(company.getId()));

        CompanyVerificationSnapshot saved = snapshotRepository.save(snapshot);
        riskAssessmentRepository.save(assessRisk(company, saved));
        checklistItemRepository.saveAll(defaultChecklist(company, saved));
        sectionReviewRepository.saveAll(defaultSections(company, saved));
        return saved;
    }

    @Transactional(readOnly = true)
    public VerificationSnapshotResponse latestSnapshot(UUID companyId) {
        return snapshotRepository.findTopByCompanyIdOrderByVersionNumberDesc(companyId)
                .map(this::toSnapshotResponse)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<VerificationChecklistItemResponse> latestChecklist(UUID companyId) {
        return latestSnapshotEntity(companyId)
                .map(snapshot -> checklistItemRepository.findBySnapshotIdOrderByItemKey(snapshot.getId())
                        .stream()
                        .map(this::toChecklistResponse)
                        .toList())
                .orElse(List.of());
    }

    @Transactional(readOnly = true)
    public List<VerificationSectionReviewResponse> latestSections(UUID companyId) {
        return latestSnapshotEntity(companyId)
                .map(snapshot -> sectionReviewRepository.findBySnapshotIdOrderBySectionKey(snapshot.getId())
                        .stream()
                        .map(this::toSectionResponse)
                        .toList())
                .orElse(List.of());
    }

    @Transactional(readOnly = true)
    public VerificationRiskResponse latestRisk(UUID companyId) {
        return riskAssessmentRepository.findTopByCompanyIdOrderByCreatedAtDesc(companyId)
                .map(this::toRiskResponse)
                .orElse(null);
    }

    @Transactional
    public VerificationChecklistItemResponse updateChecklistItem(
            UUID companyId,
            UpdateVerificationChecklistItemRequest request,
            User reviewer
    ) {
        CompanyVerificationSnapshot snapshot = latestSnapshotEntity(companyId)
                .orElseThrow(() -> new BusinessRuleException("Verification snapshot is required"));
        CompanyVerificationChecklistItem item = checklistItemRepository
                .findBySnapshotIdAndItemKey(snapshot.getId(), request.getItemKey())
                .orElseThrow(() -> new ResourceNotFoundException("Checklist item not found"));

        item.setCompleted(request.isCompleted());
        item.setNotes(request.getNotes());
        item.setReviewerUser(reviewer);
        item.setCompletedAt(request.isCompleted() ? LocalDateTime.now() : null);
        CompanyVerificationChecklistItem saved = checklistItemRepository.save(item);
        companyReviewEventService.record(
                item.getCompany(),
                reviewer,
                CompanyReviewAction.CHECKLIST_UPDATED,
                null,
                request.getItemKey(),
                request.getNotes()
        );
        return toChecklistResponse(saved);
    }

    @Transactional
    public VerificationSectionReviewResponse updateSectionReview(
            UUID companyId,
            UpdateVerificationSectionReviewRequest request,
            User reviewer
    ) {
        CompanyVerificationSnapshot snapshot = latestSnapshotEntity(companyId)
                .orElseThrow(() -> new BusinessRuleException("Verification snapshot is required"));
        CompanyVerificationSectionReview section = sectionReviewRepository
                .findBySnapshotIdAndSectionKey(snapshot.getId(), request.getSectionKey())
                .orElseThrow(() -> new ResourceNotFoundException("Review section not found"));

        section.setStatus(request.getStatus());
        section.setNotes(request.getNotes());
        section.setReviewerUser(reviewer);
        section.setReviewedAt(LocalDateTime.now());
        CompanyVerificationSectionReview saved = sectionReviewRepository.save(section);
        companyReviewEventService.record(
                section.getCompany(),
                reviewer,
                CompanyReviewAction.SECTION_REVIEWED,
                null,
                request.getSectionKey(),
                request.getNotes()
        );
        return toSectionResponse(saved);
    }

    @Transactional(readOnly = true)
    public void validateApprovalReadiness(Company company) {
        CompanyVerificationSnapshot snapshot = latestSnapshotEntity(company.getId())
                .orElseThrow(() -> new BusinessRuleException("Verification snapshot is required before approval"));

        List<String> errors = new ArrayList<>();
        if (!snapshot.isSubmissionReady()) {
            errors.add("Submitted snapshot has blocking onboarding errors");
        }
        if (snapshot.getVersionNumber() < 1) {
            errors.add("Verification snapshot version is invalid");
        }
        if (riskAssessmentRepository.findTopByCompanyIdOrderByCreatedAtDesc(company.getId()).isEmpty()) {
            errors.add("Risk assessment is required before approval");
        }
        if (checklistItemRepository.countBySnapshotIdAndMandatoryTrueAndCompletedFalse(snapshot.getId()) > 0) {
            errors.add("Mandatory verification checklist items are incomplete");
        }
        if (sectionReviewRepository.countBySnapshotIdAndStatusNot(
                snapshot.getId(),
                SectionReviewStatus.APPROVED
        ) > 0) {
            errors.add("All verification sections must be approved");
        }

        if (!errors.isEmpty()) {
            throw new BusinessRuleException("Company approval blocked: " + errors);
        }
    }

    private CompanyVerificationRiskAssessment assessRisk(
            Company company,
            CompanyVerificationSnapshot snapshot
    ) {
        List<String> signals = new ArrayList<>();
        addDuplicateSignal(signals, "Duplicate MC number", company.getMcNumber(),
                value -> companyRepository.countDuplicateMcNumber(company.getId(), value));
        addDuplicateSignal(signals, "Duplicate DOT number", company.getDotNumber(),
                value -> companyRepository.countDuplicateDotNumber(company.getId(), value));
        addDuplicateSignal(signals, "Duplicate EIN", company.getEin(),
                value -> companyRepository.countDuplicateEin(company.getId(), value));
        addDuplicateSignal(signals, "Duplicate company email", company.getEmail(),
                value -> companyRepository.countDuplicateEmail(company.getId(), value));
        addDuplicateSignal(signals, "Duplicate company phone", company.getPhone(),
                value -> companyRepository.countDuplicatePhone(company.getId(), value));
        addDuplicateSignal(signals, "Duplicate website", company.getWebsite(),
                value -> companyRepository.countDuplicateWebsite(company.getId(), value));
        addDuplicateSignal(signals, "Duplicate headquarters address", company.getHeadquarters(),
                value -> companyRepository.countDuplicateHeadquarters(company.getId(), value));

        if (company.getEmail() != null && disposableDomain(company.getEmail())) {
            signals.add("Disposable or consumer-style email domain");
        }

        int score = Math.min(100, signals.size() * 15);
        CompanyVerificationRiskAssessment assessment = new CompanyVerificationRiskAssessment();
        assessment.setCompany(company);
        assessment.setSnapshot(snapshot);
        assessment.setScore(score);
        assessment.setLevel(riskLevel(score));
        assessment.setSignals(signals);
        return assessment;
    }

    private List<CompanyVerificationChecklistItem> defaultChecklist(
            Company company,
            CompanyVerificationSnapshot snapshot
    ) {
        List<ChecklistDefinition> definitions = new ArrayList<>(List.of(
                item("company_identity_verified", "Company identity verified"),
                item("business_registration_verified", "Business registration verified"),
                item("insurance_verified", "Insurance verified"),
                item("address_verified", "Address verified"),
                item("phone_verified", "Phone verified"),
                item("email_verified", "Email verified"),
                item("required_documents_verified", "Required documents verified"),
                item("risk_reviewed", "Risk reviewed"),
                item("survey_reviewed", "Survey reviewed")
        ));

        if (company.getType() == CompanyType.FLEET) {
            definitions.add(item("dot_verified", "DOT verified"));
            definitions.add(item("mc_verified", "MC verified"));
        } else {
            definitions.add(item("broker_authority_verified", "Broker authority verified"));
        }

        return definitions.stream()
                .map(definition -> {
                    CompanyVerificationChecklistItem checklistItem =
                            new CompanyVerificationChecklistItem();
                    checklistItem.setCompany(company);
                    checklistItem.setSnapshot(snapshot);
                    checklistItem.setItemKey(definition.key());
                    checklistItem.setLabel(definition.label());
                    checklistItem.setMandatory(true);
                    checklistItem.setCompleted(false);
                    return checklistItem;
                })
                .toList();
    }

    private List<CompanyVerificationSectionReview> defaultSections(
            Company company,
            CompanyVerificationSnapshot snapshot
    ) {
        return List.of(
                        section("company_information", "Company Information"),
                        section("documents", "Documents"),
                        section("business_details", "Business Details"),
                        section("survey", "Survey"),
                        section("verification", "Verification")
                )
                .stream()
                .map(definition -> {
                    CompanyVerificationSectionReview sectionReview =
                            new CompanyVerificationSectionReview();
                    sectionReview.setCompany(company);
                    sectionReview.setSnapshot(snapshot);
                    sectionReview.setSectionKey(definition.key());
                    sectionReview.setLabel(definition.label());
                    sectionReview.setStatus(SectionReviewStatus.PENDING);
                    return sectionReview;
                })
                .toList();
    }

    private java.util.Optional<CompanyVerificationSnapshot> latestSnapshotEntity(UUID companyId) {
        return snapshotRepository.findTopByCompanyIdOrderByVersionNumberDesc(companyId);
    }

    private VerificationSnapshotResponse toSnapshotResponse(CompanyVerificationSnapshot snapshot) {
        return new VerificationSnapshotResponse(
                snapshot.getId(),
                snapshot.getVersionNumber(),
                snapshot.getCompanyType(),
                snapshot.getUserStatus(),
                snapshot.getCompanyVerificationStatus(),
                snapshot.getSubmittedAt(),
                snapshot.getCompletionPercentage(),
                snapshot.isSubmissionReady(),
                nullToEmpty(snapshot.getCompletedSections()),
                nullToEmpty(snapshot.getIncompleteSections()),
                nullToEmpty(snapshot.getMissingFields()),
                nullToEmpty(snapshot.getMissingDocuments()),
                nullToEmpty(snapshot.getInvalidFields()),
                nullToEmpty(snapshot.getWarnings()),
                nullToEmpty(snapshot.getBlockingErrors()),
                snapshot.getLegalName(),
                snapshot.getEmail(),
                snapshot.getPhone(),
                snapshot.getMcNumber(),
                snapshot.getDotNumber(),
                snapshot.getEin(),
                snapshot.getHeadquarters(),
                snapshot.getWebsite(),
                snapshot.getFleetSize(),
                snapshot.getActiveVehicleCount()
        );
    }

    private VerificationRiskResponse toRiskResponse(CompanyVerificationRiskAssessment assessment) {
        return new VerificationRiskResponse(
                assessment.getId(),
                assessment.getScore(),
                assessment.getLevel(),
                nullToEmpty(assessment.getSignals())
        );
    }

    private VerificationChecklistItemResponse toChecklistResponse(CompanyVerificationChecklistItem item) {
        return new VerificationChecklistItemResponse(
                item.getId(),
                item.getItemKey(),
                item.getLabel(),
                item.isMandatory(),
                item.isCompleted(),
                item.getNotes(),
                item.getReviewerUser() == null ? null : item.getReviewerUser().getId(),
                item.getCompletedAt()
        );
    }

    private VerificationSectionReviewResponse toSectionResponse(CompanyVerificationSectionReview section) {
        return new VerificationSectionReviewResponse(
                section.getId(),
                section.getSectionKey(),
                section.getLabel(),
                section.getStatus(),
                section.getNotes(),
                section.getReviewerUser() == null ? null : section.getReviewerUser().getId(),
                section.getReviewedAt()
        );
    }

    private void addDuplicateSignal(
            List<String> signals,
            String label,
            String value,
            java.util.function.Function<String, Long> counter
    ) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (counter.apply(value) > 0) {
            signals.add(label);
        }
    }

    private boolean disposableDomain(String email) {
        String domain = email.substring(email.indexOf('@') + 1).toLowerCase();
        return List.of("gmail.com", "yahoo.com", "hotmail.com", "outlook.com", "mailinator.com")
                .contains(domain);
    }

    private VerificationRiskLevel riskLevel(int score) {
        if (score >= 75) {
            return VerificationRiskLevel.CRITICAL;
        }
        if (score >= 45) {
            return VerificationRiskLevel.HIGH;
        }
        if (score >= 20) {
            return VerificationRiskLevel.MEDIUM;
        }
        return VerificationRiskLevel.LOW;
    }

    private ChecklistDefinition item(String key, String label) {
        return new ChecklistDefinition(key, label);
    }

    private SectionDefinition section(String key, String label) {
        return new SectionDefinition(key, label);
    }

    private List<String> nullToEmpty(List<String> values) {
        return values == null ? List.of() : values;
    }

    private record ChecklistDefinition(String key, String label) {
    }

    private record SectionDefinition(String key, String label) {
    }
}
