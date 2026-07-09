package com.fleetmatch.onboarding.service;

import com.fleetmatch.common.exception.BusinessRuleException;
import com.fleetmatch.common.exception.ResourceNotFoundException;
import com.fleetmatch.company.documents.dto.CompanyDocumentResponse;
import com.fleetmatch.company.documents.entity.CompanyDocument;
import com.fleetmatch.company.documents.entity.DocumentReviewStatus;
import com.fleetmatch.company.documents.entity.DocumentType;
import com.fleetmatch.company.documents.repository.CompanyDocumentRepository;
import com.fleetmatch.company.entity.Company;
import com.fleetmatch.company.entity.CompanyType;
import com.fleetmatch.company.entity.CompanyVerificationStatus;
import com.fleetmatch.company.repository.CompanyRepository;
import com.fleetmatch.company.review.entity.CompanyReviewAction;
import com.fleetmatch.company.review.service.CompanyReviewEventService;
import com.fleetmatch.onboarding.dto.MarketSurveyRequest;
import com.fleetmatch.onboarding.dto.OnboardingDocumentRequirement;
import com.fleetmatch.onboarding.dto.OnboardingFieldRequirement;
import com.fleetmatch.onboarding.dto.OnboardingPreviewResponse;
import com.fleetmatch.onboarding.dto.OnboardingProgressResponse;
import com.fleetmatch.onboarding.dto.OnboardingSectionResponse;
import com.fleetmatch.onboarding.dto.OnboardingValidationResponse;
import com.fleetmatch.onboarding.entity.MarketSurvey;
import com.fleetmatch.onboarding.exception.OnboardingValidationException;
import com.fleetmatch.onboarding.repository.MarketSurveyRepository;
import com.fleetmatch.security.user.CustomUserDetails;
import com.fleetmatch.user.entity.User;
import com.fleetmatch.user.entity.UserStatus;
import com.fleetmatch.user.repository.UserRepository;
import com.fleetmatch.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private static final String ESTIMATED_REVIEW_TIME = "1-2 business days";

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final CompanyDocumentRepository companyDocumentRepository;
    private final MarketSurveyRepository marketSurveyRepository;
    private final CompanyReviewEventService companyReviewEventService;
    private final VehicleRepository vehicleRepository;

    @Transactional(readOnly = true)
    public OnboardingProgressResponse getProgress(CustomUserDetails currentUser) {
        User user = getUser(currentUser);
        Company company = requireCompany(user);

        OnboardingValidationResponse validation = validateApplication(user, company);

        return new OnboardingProgressResponse(
                user.getStatus(),
                company.getType(),
                company.getVerificationStatus(),
                user.isEmailVerified(),
                phoneStepComplete(user),
                company.isCompanyInformationCompleted(),
                validation.getMissingDocuments().isEmpty(),
                company.isMarketSurveyCompleted(),
                company.getVerificationStatus() == CompanyVerificationStatus.UNDER_REVIEW ||
                        user.getStatus() == UserStatus.IN_REVIEW,
                company.getVerificationStatus() == CompanyVerificationStatus.APPROVED &&
                        (user.getStatus() == UserStatus.APPROVED ||
                                user.getStatus() == UserStatus.ACTIVE),
                ESTIMATED_REVIEW_TIME,
                progressMessage(user, company),
                validation.getCompletionPercentage(),
                validation.getCompletedSections(),
                validation.getIncompleteSections(),
                validation.getSections(),
                validation.getRequiredFields(),
                validation.getRequiredDocuments(),
                validation.getMissingFields(),
                validation.getMissingDocuments(),
                validation.getInvalidFields(),
                validation.getWarnings(),
                validation.getBlockingErrors(),
                validation.isSubmissionReady()
        );
    }

    @Transactional(readOnly = true)
    public OnboardingValidationResponse validate(CustomUserDetails currentUser) {
        User user = getUser(currentUser);
        Company company = requireCompany(user);
        return validateApplication(user, company);
    }

    @Transactional(readOnly = true)
    public OnboardingPreviewResponse preview(CustomUserDetails currentUser) {
        User user = getUser(currentUser);
        Company company = requireCompany(user);
        List<CompanyDocument> documents = companyDocumentRepository.findByCompanyId(company.getId());
        MarketSurvey survey = marketSurveyRepository.findByCompanyId(company.getId()).orElse(null);

        return new OnboardingPreviewResponse(
                new OnboardingPreviewResponse.CompanyPreview(
                        company.getId(),
                        company.getLegalName(),
                        company.getDbaName(),
                        company.getType(),
                        company.getVerificationStatus(),
                        company.getEntityType(),
                        company.getEin(),
                        company.getStateOfFormation(),
                        company.getHeadquarters(),
                        company.getMcNumber(),
                        company.getDotNumber(),
                        company.getAuthorityStatus(),
                        company.getBrokerBondOrTrust(),
                        company.getInsuranceCoverage(),
                        company.getOperatingRegions(),
                        company.getFleetSize(),
                        vehicleRepository.countByCompanyIdAndActiveTrue(company.getId())
                ),
                new OnboardingPreviewResponse.ContactPreview(
                        company.getPrimaryContact(),
                        company.getEmail(),
                        company.getPhone(),
                        company.getWebsite(),
                        user.isEmailVerified(),
                        phoneStepComplete(user)
                ),
                documents.stream().map(this::toDocumentResponse).toList(),
                toSurveyPreview(survey),
                validateApplication(user, company)
        );
    }

    @Transactional
    public OnboardingProgressResponse submitSurvey(
            MarketSurveyRequest request,
            CustomUserDetails currentUser
    ) {
        User user = getUser(currentUser);
        Company company = requireCompany(user);

        ensureOnboardingEditable(user, company);
        validateSurveyRequest(request, company.getType());

        MarketSurvey survey = marketSurveyRepository.findByCompanyId(company.getId())
                .orElseGet(MarketSurvey::new);

        survey.setCompany(company);
        survey.setCompanyType(company.getType());
        survey.setOperatingStates(request.getOperatingStates());
        survey.setEquipmentTypes(request.getEquipmentTypes());
        survey.setAverageLoadsPerWeek(request.getAverageLoadsPerWeek());
        survey.setFleetSize(request.getFleetSize());
        survey.setCurrentLoadBoard(request.getCurrentLoadBoard());
        survey.setCurrentTms(request.getCurrentTms());
        survey.setFutureIntegrationInterest(request.getFutureIntegrationInterest());
        survey.setBiggestOperationalChallenges(request.getBiggestOperationalChallenges());
        survey.setHomeState(request.getHomeState());
        survey.setPreferredRegions(request.getPreferredRegions());
        survey.setPreferredMileage(request.getPreferredMileage());
        survey.setDedicatedRouteInterest(request.getDedicatedRouteInterest());

        marketSurveyRepository.save(survey);

        company.setMarketSurveyCompleted(true);
        companyRepository.save(company);
        advanceStatusAfterOnboardingInput(user, company);

        return getProgress(currentUser);
    }

    @Transactional
    public OnboardingProgressResponse submitForReview(CustomUserDetails currentUser) {
        User user = getUser(currentUser);
        Company company = requireCompany(user);

        if (user.getStatus() == UserStatus.IN_REVIEW ||
                company.getVerificationStatus() == CompanyVerificationStatus.UNDER_REVIEW) {
            return getProgress(currentUser);
        }

        OnboardingValidationResponse validation = validateApplication(user, company);
        if (!validation.isSubmissionReady()) {
            throw new OnboardingValidationException(validation);
        }

        company.setVerificationStatus(CompanyVerificationStatus.UNDER_REVIEW);
        user.setStatus(UserStatus.IN_REVIEW);
        companyRepository.save(company);
        userRepository.save(user);
        companyReviewEventService.record(
                company,
                user,
                CompanyReviewAction.SUBMITTED_FOR_REVIEW,
                null,
                null,
                "Company submitted for operational review"
        );

        return getProgress(currentUser);
    }

    public void advanceStatusAfterOnboardingInput(User user, Company company) {
        if (user.getStatus() == UserStatus.APPROVED ||
                user.getStatus() == UserStatus.ACTIVE ||
                user.getStatus() == UserStatus.SUSPENDED) {
            return;
        }

        if (requiredDocumentsUploaded(company) &&
                company.isCompanyInformationCompleted() &&
                company.isMarketSurveyCompleted()) {
            user.setStatus(UserStatus.DOCUMENTS_PENDING);
            userRepository.save(user);
        }
    }

    private User getUser(CustomUserDetails currentUser) {
        return userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Company requireCompany(User user) {
        if (user.getCompany() == null) {
            throw new BusinessRuleException("Company is required for onboarding");
        }
        return user.getCompany();
    }

    private void ensureOnboardingEditable(User user, Company company) {
        if (user.getStatus() == UserStatus.IN_REVIEW ||
                company.getVerificationStatus() == CompanyVerificationStatus.UNDER_REVIEW) {
            throw new BusinessRuleException(
                    "Onboarding is locked during admin review. Contact support for urgent corrections."
            );
        }
    }

    private boolean phoneStepComplete(User user) {
        return user.getPhone() == null ||
                user.getPhone().isBlank() ||
                user.isPhoneVerified();
    }

    private String progressMessage(User user, Company company) {
        if (company.getVerificationStatus() == CompanyVerificationStatus.APPROVED) {
            return "Your company has been approved. Subscription selection is now available.";
        }
        if (company.getVerificationStatus() == CompanyVerificationStatus.REJECTED) {
            return "Your verification could not be completed. Please review the requested updates and submit again.";
        }
        if (user.getStatus() == UserStatus.IN_REVIEW ||
                company.getVerificationStatus() == CompanyVerificationStatus.UNDER_REVIEW) {
            return "Your company is in review. We will notify you when approval is complete.";
        }
        return "Complete each onboarding step to submit your company for review.";
    }

    private OnboardingValidationResponse validateApplication(User user, Company company) {
        List<OnboardingFieldRequirement> requiredFields = requiredFields(company.getType());
        List<OnboardingDocumentRequirement> requiredDocuments = requiredDocuments(company.getType());
        List<String> missingFields = missingFields(user, company);
        List<DocumentType> missingDocuments = missingDocuments(company, requiredDocuments);
        List<String> invalidFields = invalidFields(company);
        marketSurveyRepository.findByCompanyId(company.getId())
                .ifPresentOrElse(
                        survey -> {
                            missingFields.addAll(missingSurveyFields(survey, company.getType()));
                            invalidFields.addAll(invalidSurveyFields(survey));
                        },
                        () -> {
                            missingFields.addAll(surveyRequirementKeys(company.getType()));
                        }
                );
        List<String> warnings = warnings(company);
        List<String> blockingErrors = blockingErrors(
                user,
                company,
                missingFields,
                missingDocuments,
                invalidFields
        );
        List<OnboardingSectionResponse> sections = sections(
                user,
                company,
                missingFields,
                missingDocuments,
                invalidFields,
                warnings
        );
        List<String> completedSections = sections.stream()
                .filter(OnboardingSectionResponse::isComplete)
                .map(OnboardingSectionResponse::getKey)
                .toList();
        List<String> incompleteSections = sections.stream()
                .filter(section -> !section.isComplete())
                .map(OnboardingSectionResponse::getKey)
                .toList();
        int completionPercentage = sections.isEmpty()
                ? 0
                : sections.stream()
                .mapToInt(OnboardingSectionResponse::getCompletionPercentage)
                .sum() / sections.size();

        return new OnboardingValidationResponse(
                company.getType(),
                blockingErrors.isEmpty(),
                completionPercentage,
                completedSections,
                incompleteSections,
                sections,
                requiredFields,
                requiredDocuments,
                missingFields,
                missingDocuments,
                invalidFields,
                warnings,
                blockingErrors,
                ESTIMATED_REVIEW_TIME
        );
    }

    private List<OnboardingFieldRequirement> requiredFields(CompanyType type) {
        List<OnboardingFieldRequirement> requirements = new ArrayList<>();
        requirements.add(field("legalName", "Legal company name"));
        requirements.add(field("email", "Company email"));
        requirements.add(field("phone", "Company phone"));
        requirements.add(field("primaryContact", "Primary contact"));
        requirements.add(field("entityType", "Entity type"));
        requirements.add(field("ein", "EIN"));
        requirements.add(field("stateOfFormation", "State of formation"));
        requirements.add(field("headquarters", "Headquarters address"));
        requirements.add(field("mcNumber", "MC authority number"));
        requirements.add(field("insuranceCoverage", "Insurance coverage"));
        requirements.add(field("operatingRegions", "Operating regions"));

        if (type == CompanyType.FLEET) {
            requirements.add(field("dotNumber", "DOT number"));
            requirements.add(field("fleetSize", "Fleet size"));
            requirements.add(field("vehicleInformation", "Vehicle information"));
        }

        if (type == CompanyType.BROKER) {
            requirements.add(field("authorityStatus", "Broker authority status"));
            requirements.add(field("brokerBondOrTrust", "Broker bond or trust"));
        }

        return requirements;
    }

    private List<OnboardingDocumentRequirement> requiredDocuments(CompanyType type) {
        if (type == CompanyType.BROKER) {
            return List.of(
                    document(DocumentType.BUSINESS_REGISTRATION, "Business registration"),
                    document(DocumentType.CERTIFICATE_OF_INSURANCE, "Insurance certificate"),
                    document(DocumentType.MC_AUTHORITY, "Broker authority")
            );
        }

        return List.of(
                document(DocumentType.DOT_REGISTRATION, "DOT authority"),
                document(DocumentType.MC_AUTHORITY, "MC authority"),
                document(DocumentType.CERTIFICATE_OF_INSURANCE, "Insurance certificate"),
                document(DocumentType.BUSINESS_REGISTRATION, "Business registration")
        );
    }

    private List<String> missingFields(User user, Company company) {
        List<String> missing = new ArrayList<>();
        requireText(missing, "legalName", company.getLegalName());
        requireText(missing, "email", company.getEmail());
        requireText(missing, "phone", company.getPhone());
        requireText(missing, "primaryContact", company.getPrimaryContact());
        requireText(missing, "entityType", company.getEntityType());
        requireText(missing, "ein", company.getEin());
        requireText(missing, "stateOfFormation", company.getStateOfFormation());
        requireText(missing, "headquarters", company.getHeadquarters());
        requireText(missing, "mcNumber", company.getMcNumber());
        requireText(missing, "insuranceCoverage", company.getInsuranceCoverage());
        requireText(missing, "operatingRegions", company.getOperatingRegions());

        if (company.getType() == CompanyType.FLEET) {
            requireText(missing, "dotNumber", company.getDotNumber());
            if (company.getFleetSize() == null || company.getFleetSize() <= 0) {
                missing.add("fleetSize");
            }
            if (vehicleRepository.countByCompanyIdAndActiveTrue(company.getId()) == 0) {
                missing.add("vehicleInformation");
            }
        }

        if (company.getType() == CompanyType.BROKER) {
            requireText(missing, "authorityStatus", company.getAuthorityStatus());
            requireText(missing, "brokerBondOrTrust", company.getBrokerBondOrTrust());
        }

        if (!user.isEmailVerified()) {
            missing.add("emailVerification");
        }
        if (!phoneStepComplete(user)) {
            missing.add("phoneVerification");
        }
        if (!company.isMarketSurveyCompleted()) {
            missing.add("marketSurvey");
        }

        return missing;
    }

    private List<DocumentType> missingDocuments(
            Company company,
            List<OnboardingDocumentRequirement> requiredDocuments
    ) {
        Set<DocumentType> uploadedTypes = companyDocumentRepository
                .findByCompanyId(company.getId())
                .stream()
                .filter(this::isUsableForSubmission)
                .map(CompanyDocument::getDocumentType)
                .collect(java.util.stream.Collectors.toCollection(() -> EnumSet.noneOf(DocumentType.class)));

        return requiredDocuments.stream()
                .map(OnboardingDocumentRequirement::getDocumentType)
                .filter(required -> !uploadedTypes.contains(required))
                .toList();
    }

    private List<String> invalidFields(Company company) {
        List<String> invalid = new ArrayList<>();
        if (!isBlank(company.getMcNumber()) &&
                !company.getMcNumber().matches("^MC-\\d{5,8}$")) {
            invalid.add("mcNumber");
        }
        if (company.getType() == CompanyType.FLEET &&
                company.getFleetSize() != null &&
                company.getFleetSize() <= 0) {
            invalid.add("fleetSize");
        }
        return invalid;
    }

    private List<String> warnings(Company company) {
        List<String> warnings = new ArrayList<>();
        if (!company.isHeadquartersAddressVerified()) {
            warnings.add("Headquarters address has not been verified yet");
        }
        if (company.getType() == CompanyType.BROKER && !isBlank(company.getDotNumber())) {
            warnings.add("DOT number is not required for broker onboarding");
        }
        return warnings;
    }

    private List<String> blockingErrors(
            User user,
            Company company,
            List<String> missingFields,
            List<DocumentType> missingDocuments,
            List<String> invalidFields
    ) {
        List<String> errors = new ArrayList<>();
        if (!user.isEmailVerified()) {
            errors.add("Email must be verified before review");
        }
        if (!phoneStepComplete(user)) {
            errors.add("Phone must be verified before review");
        }
        if (!missingFields.isEmpty()) {
            errors.add("Required onboarding fields are incomplete");
        }
        if (!missingDocuments.isEmpty()) {
            errors.add("Required documents are incomplete");
        }
        if (!invalidFields.isEmpty()) {
            errors.add("Some onboarding fields are invalid");
        }
        if (!company.isMarketSurveyCompleted()) {
            errors.add("Market survey must be completed before review");
        }
        return errors.stream().distinct().toList();
    }

    private List<OnboardingSectionResponse> sections(
            User user,
            Company company,
            List<String> missingFields,
            List<DocumentType> missingDocuments,
            List<String> invalidFields,
            List<String> warnings
    ) {
        return List.of(
                section(
                        "verification",
                        "Verification",
                        List.of("emailVerification", "phoneVerification"),
                        missingFields,
                        List.of(),
                        List.of()
                ),
                section(
                        "company_information",
                        "Company Information",
                        requiredFields(company.getType()).stream()
                                .map(OnboardingFieldRequirement::getKey)
                                .filter(key -> !key.equals("vehicleInformation"))
                                .toList(),
                        missingFields,
                        invalidFields,
                        warnings
                ),
                section(
                        "documents",
                        "Documents",
                        requiredDocuments(company.getType()).stream()
                                .map(requirement -> requirement.getDocumentType().name())
                                .toList(),
                        missingDocuments.stream().map(Enum::name).toList(),
                        List.of(),
                        List.of()
                ),
                section(
                        "survey",
                        "Market Survey",
                        surveyRequirementKeys(company.getType()),
                        missingFields,
                        invalidFields,
                        List.of()
                ),
                section(
                        "profile",
                        "Profile",
                        company.getType() == CompanyType.FLEET
                                ? List.of("vehicleInformation")
                                : List.of("brokerBusinessProfile"),
                        company.getType() == CompanyType.FLEET
                                ? missingFields
                                : List.of(),
                        List.of(),
                        List.of()
                )
        );
    }

    private OnboardingSectionResponse section(
            String key,
            String label,
            List<String> requiredItems,
            List<String> missingItems,
            List<String> invalidItems,
            List<String> warnings
    ) {
        List<String> missing = requiredItems.stream()
                .filter(item -> missingItems.contains(item) || invalidItems.contains(item))
                .toList();
        List<String> completed = requiredItems.stream()
                .filter(item -> !missing.contains(item))
                .toList();
        int percentage = requiredItems.isEmpty()
                ? 100
                : (completed.size() * 100) / requiredItems.size();
        return new OnboardingSectionResponse(
                key,
                label,
                percentage,
                missing.isEmpty(),
                completed,
                missing,
                warnings
        );
    }

    private boolean requiredDocumentsUploaded(Company company) {
        return missingDocuments(company, requiredDocuments(company.getType())).isEmpty();
    }

    private void validateSurveyRequest(MarketSurveyRequest request, CompanyType companyType) {
        List<String> missing = missingSurveyFields(request, companyType);
        List<String> invalid = invalidSurveyFields(request);
        if (!missing.isEmpty() || !invalid.isEmpty()) {
            throw new BusinessRuleException(
                    "Market survey is incomplete. Missing: " + missing + "; Invalid: " + invalid
            );
        }
    }

    private List<String> missingSurveyFields(MarketSurveyRequest request, CompanyType companyType) {
        List<String> missing = new ArrayList<>();
        requireList(missing, "survey.operatingStates", request.getOperatingStates());
        requireList(missing, "survey.equipmentTypes", request.getEquipmentTypes());
        requirePositiveInteger(missing, "survey.averageLoadsPerWeek", request.getAverageLoadsPerWeek());
        requireText(missing, "survey.biggestOperationalChallenges", request.getBiggestOperationalChallenges());

        if (companyType == CompanyType.FLEET) {
            requirePositiveInteger(missing, "survey.fleetSize", request.getFleetSize());
            requireText(missing, "survey.currentLoadBoard", request.getCurrentLoadBoard());
        }

        if (companyType == CompanyType.BROKER) {
            requireText(missing, "survey.currentLoadBoard", request.getCurrentLoadBoard());
            requireText(missing, "survey.currentTms", request.getCurrentTms());
            if (request.getFutureIntegrationInterest() == null) {
                missing.add("survey.futureIntegrationInterest");
            }
        }

        return missing;
    }

    private List<String> missingSurveyFields(MarketSurvey survey, CompanyType companyType) {
        MarketSurveyRequest request = new MarketSurveyRequest();
        request.setOperatingStates(survey.getOperatingStates());
        request.setEquipmentTypes(survey.getEquipmentTypes());
        request.setAverageLoadsPerWeek(survey.getAverageLoadsPerWeek());
        request.setFleetSize(survey.getFleetSize());
        request.setCurrentLoadBoard(survey.getCurrentLoadBoard());
        request.setCurrentTms(survey.getCurrentTms());
        request.setFutureIntegrationInterest(survey.getFutureIntegrationInterest());
        request.setBiggestOperationalChallenges(survey.getBiggestOperationalChallenges());
        return missingSurveyFields(request, companyType);
    }

    private List<String> invalidSurveyFields(MarketSurveyRequest request) {
        List<String> invalid = new ArrayList<>();
        if (request.getAverageLoadsPerWeek() != null && request.getAverageLoadsPerWeek() <= 0) {
            invalid.add("survey.averageLoadsPerWeek");
        }
        if (request.getFleetSize() != null && request.getFleetSize() <= 0) {
            invalid.add("survey.fleetSize");
        }
        return invalid;
    }

    private List<String> invalidSurveyFields(MarketSurvey survey) {
        MarketSurveyRequest request = new MarketSurveyRequest();
        request.setAverageLoadsPerWeek(survey.getAverageLoadsPerWeek());
        request.setFleetSize(survey.getFleetSize());
        return invalidSurveyFields(request);
    }

    private List<String> surveyRequirementKeys(CompanyType companyType) {
        List<String> keys = new ArrayList<>(List.of(
                "survey.operatingStates",
                "survey.equipmentTypes",
                "survey.averageLoadsPerWeek",
                "survey.biggestOperationalChallenges"
        ));

        if (companyType == CompanyType.FLEET) {
            keys.add("survey.fleetSize");
            keys.add("survey.currentLoadBoard");
        }

        if (companyType == CompanyType.BROKER) {
            keys.add("survey.currentLoadBoard");
            keys.add("survey.currentTms");
            keys.add("survey.futureIntegrationInterest");
        }

        return keys;
    }

    private boolean isUsableForSubmission(CompanyDocument document) {
        return document.getReviewStatus() == DocumentReviewStatus.PENDING ||
                document.getReviewStatus() == DocumentReviewStatus.APPROVED;
    }

    private OnboardingFieldRequirement field(String key, String label) {
        return new OnboardingFieldRequirement(key, label, true);
    }

    private OnboardingDocumentRequirement document(DocumentType documentType, String label) {
        return new OnboardingDocumentRequirement(documentType, label, true);
    }

    private void requireText(List<String> missing, String key, String value) {
        if (isBlank(value)) {
            missing.add(key);
        }
    }

    private void requireList(List<String> missing, String key, List<String> value) {
        if (value == null || value.isEmpty()) {
            missing.add(key);
        }
    }

    private void requirePositiveInteger(List<String> missing, String key, Integer value) {
        if (value == null || value <= 0) {
            missing.add(key);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private CompanyDocumentResponse toDocumentResponse(CompanyDocument document) {
        return new CompanyDocumentResponse(
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
        );
    }

    private OnboardingPreviewResponse.SurveyPreview toSurveyPreview(MarketSurvey survey) {
        if (survey == null) {
            return null;
        }
        return new OnboardingPreviewResponse.SurveyPreview(
                survey.getOperatingStates(),
                survey.getEquipmentTypes(),
                survey.getAverageLoadsPerWeek(),
                survey.getFleetSize(),
                survey.getCurrentLoadBoard(),
                survey.getCurrentTms(),
                survey.getFutureIntegrationInterest(),
                survey.getBiggestOperationalChallenges(),
                survey.getHomeState(),
                survey.getPreferredRegions(),
                survey.getPreferredMileage(),
                survey.getDedicatedRouteInterest()
        );
    }
}
