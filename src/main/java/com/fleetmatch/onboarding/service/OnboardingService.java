package com.fleetmatch.onboarding.service;

import com.fleetmatch.common.exception.BusinessRuleException;
import com.fleetmatch.common.exception.ResourceNotFoundException;
import com.fleetmatch.company.documents.repository.CompanyDocumentRepository;
import com.fleetmatch.company.entity.Company;
import com.fleetmatch.company.entity.CompanyVerificationStatus;
import com.fleetmatch.company.repository.CompanyRepository;
import com.fleetmatch.company.review.entity.CompanyReviewAction;
import com.fleetmatch.company.review.service.CompanyReviewEventService;
import com.fleetmatch.onboarding.dto.MarketSurveyRequest;
import com.fleetmatch.onboarding.dto.OnboardingProgressResponse;
import com.fleetmatch.onboarding.entity.MarketSurvey;
import com.fleetmatch.onboarding.repository.MarketSurveyRepository;
import com.fleetmatch.security.user.CustomUserDetails;
import com.fleetmatch.user.entity.User;
import com.fleetmatch.user.entity.UserStatus;
import com.fleetmatch.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private static final String ESTIMATED_REVIEW_TIME = "1-2 business days";

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final CompanyDocumentRepository companyDocumentRepository;
    private final MarketSurveyRepository marketSurveyRepository;
    private final CompanyReviewEventService companyReviewEventService;

    @Transactional(readOnly = true)
    public OnboardingProgressResponse getProgress(CustomUserDetails currentUser) {
        User user = getUser(currentUser);
        Company company = requireCompany(user);

        boolean documentsUploaded =
                companyDocumentRepository.existsByCompanyId(company.getId());

        return new OnboardingProgressResponse(
                user.getStatus(),
                company.getType(),
                company.getVerificationStatus(),
                user.isEmailVerified(),
                phoneStepComplete(user),
                company.isCompanyInformationCompleted(),
                documentsUploaded,
                company.isMarketSurveyCompleted(),
                company.getVerificationStatus() == CompanyVerificationStatus.UNDER_REVIEW ||
                        user.getStatus() == UserStatus.IN_REVIEW,
                company.getVerificationStatus() == CompanyVerificationStatus.APPROVED &&
                        (user.getStatus() == UserStatus.APPROVED ||
                                user.getStatus() == UserStatus.ACTIVE),
                ESTIMATED_REVIEW_TIME,
                progressMessage(user, company)
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

        if (!user.isEmailVerified()) {
            throw new BusinessRuleException("Email must be verified before review");
        }

        if (!phoneStepComplete(user)) {
            throw new BusinessRuleException("Phone must be verified before review");
        }

        if (!company.isCompanyInformationCompleted()) {
            throw new BusinessRuleException("Company information must be completed before review");
        }

        if (!companyDocumentRepository.existsByCompanyId(company.getId())) {
            throw new BusinessRuleException("Required documents must be uploaded before review");
        }

        if (!company.isMarketSurveyCompleted()) {
            throw new BusinessRuleException("Market survey must be completed before review");
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

        if (companyDocumentRepository.existsByCompanyId(company.getId()) &&
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
}
