package com.fleetmatch.onboarding.service;

import com.fleetmatch.common.exception.BusinessRuleException;
import com.fleetmatch.company.document.repository.CompanyDocumentRepository;
import com.fleetmatch.company.entity.Company;
import com.fleetmatch.company.entity.CompanyType;
import com.fleetmatch.company.entity.CompanyVerificationStatus;
import com.fleetmatch.company.repository.CompanyRepository;
import com.fleetmatch.company.review.service.CompanyReviewEventService;
import com.fleetmatch.onboarding.repository.MarketSurveyRepository;
import com.fleetmatch.security.user.CustomUserDetails;
import com.fleetmatch.user.entity.User;
import com.fleetmatch.user.entity.UserStatus;
import com.fleetmatch.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnboardingSubmitForReviewTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private CompanyDocumentRepository companyDocumentRepository;
    @Mock
    private MarketSurveyRepository marketSurveyRepository;
    @Mock
    private CompanyReviewEventService companyReviewEventService;

    @InjectMocks
    private OnboardingService onboardingService;

    @Test
    void submitForReviewBlocksUntilEmailIsVerified() {
        Fixture fixture = fixture();
        fixture.user.setEmailVerified(false);
        when(userRepository.findById(fixture.user.getId())).thenReturn(Optional.of(fixture.user));

        assertThrows(
                BusinessRuleException.class,
                () -> onboardingService.submitForReview(new CustomUserDetails(fixture.user))
        );

        verify(companyRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void submitForReviewBlocksUntilDocumentsAreUploaded() {
        Fixture fixture = fixture();
        when(userRepository.findById(fixture.user.getId())).thenReturn(Optional.of(fixture.user));
        when(companyDocumentRepository.existsByCompanyId(fixture.company.getId())).thenReturn(false);

        assertThrows(
                BusinessRuleException.class,
                () -> onboardingService.submitForReview(new CustomUserDetails(fixture.user))
        );

        verify(companyRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void submitForReviewMovesCompleteOnboardingIntoReview() {
        Fixture fixture = fixture();
        when(userRepository.findById(fixture.user.getId())).thenReturn(Optional.of(fixture.user));
        when(companyDocumentRepository.existsByCompanyId(fixture.company.getId())).thenReturn(true);

        onboardingService.submitForReview(new CustomUserDetails(fixture.user));

        assertEquals(UserStatus.IN_REVIEW, fixture.user.getStatus());
        assertEquals(CompanyVerificationStatus.UNDER_REVIEW, fixture.company.getVerificationStatus());
        verify(companyRepository).save(fixture.company);
        verify(userRepository).save(fixture.user);
        verify(companyReviewEventService).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    void submitForReviewIsIdempotentWhileAlreadyInReview() {
        Fixture fixture = fixture();
        fixture.user.setStatus(UserStatus.IN_REVIEW);
        fixture.company.setVerificationStatus(CompanyVerificationStatus.UNDER_REVIEW);
        when(userRepository.findById(fixture.user.getId())).thenReturn(Optional.of(fixture.user));
        when(companyDocumentRepository.existsByCompanyId(fixture.company.getId())).thenReturn(true);

        onboardingService.submitForReview(new CustomUserDetails(fixture.user));

        verify(companyRepository, never()).save(any());
        verify(userRepository, never()).save(any());
        verify(companyReviewEventService, never()).record(any(), any(), any(), any(), any(), any());
    }

    private Fixture fixture() {
        Company company = new Company();
        company.setId(UUID.randomUUID());
        company.setLegalName("Ready Logistics LLC");
        company.setEmail("ops@ready.test");
        company.setType(CompanyType.FLEET);
        company.setVerificationStatus(CompanyVerificationStatus.PENDING);
        company.setCompanyInformationCompleted(true);
        company.setMarketSurveyCompleted(true);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setFirstName("Ready");
        user.setLastName("Owner");
        user.setEmail("owner@ready.test");
        user.setPhone("+15550123");
        user.setPassword("encoded-password");
        user.setEmailVerified(true);
        user.setPhoneVerified(true);
        user.setStatus(UserStatus.PHONE_VERIFIED);
        user.setCompany(company);

        return new Fixture(user, company);
    }

    private record Fixture(User user, Company company) {
    }
}
