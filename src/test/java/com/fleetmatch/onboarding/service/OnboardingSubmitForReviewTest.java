package com.fleetmatch.onboarding.service;

import com.fleetmatch.company.documents.entity.CompanyDocument;
import com.fleetmatch.company.documents.entity.DocumentType;
import com.fleetmatch.company.documents.repository.CompanyDocumentRepository;
import com.fleetmatch.company.entity.Company;
import com.fleetmatch.company.entity.CompanyType;
import com.fleetmatch.company.entity.CompanyVerificationStatus;
import com.fleetmatch.onboarding.exception.OnboardingValidationException;
import com.fleetmatch.company.repository.CompanyRepository;
import com.fleetmatch.company.review.service.CompanyReviewEventService;
import com.fleetmatch.onboarding.repository.MarketSurveyRepository;
import com.fleetmatch.security.user.CustomUserDetails;
import com.fleetmatch.user.entity.User;
import com.fleetmatch.user.entity.UserStatus;
import com.fleetmatch.user.repository.UserRepository;
import com.fleetmatch.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

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
    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private OnboardingService onboardingService;

    @Test
    void submitForReviewBlocksUntilEmailIsVerified() {
        Fixture fixture = fixture();
        fixture.user.setEmailVerified(false);
        when(userRepository.findById(fixture.user.getId())).thenReturn(Optional.of(fixture.user));
        when(companyDocumentRepository.findByCompanyId(fixture.company.getId())).thenReturn(requiredFleetDocuments(fixture.company));
        when(vehicleRepository.countByCompanyIdAndActiveTrue(fixture.company.getId())).thenReturn(1L);

        assertThrows(
                OnboardingValidationException.class,
                () -> onboardingService.submitForReview(new CustomUserDetails(fixture.user))
        );

        verify(companyRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void submitForReviewBlocksUntilDocumentsAreUploaded() {
        Fixture fixture = fixture();
        when(userRepository.findById(fixture.user.getId())).thenReturn(Optional.of(fixture.user));
        when(companyDocumentRepository.findByCompanyId(fixture.company.getId())).thenReturn(List.of());
        when(vehicleRepository.countByCompanyIdAndActiveTrue(fixture.company.getId())).thenReturn(1L);

        OnboardingValidationException exception = assertThrows(
                OnboardingValidationException.class,
                () -> onboardingService.submitForReview(new CustomUserDetails(fixture.user))
        );

        assertEquals(4, exception.getValidation().getMissingDocuments().size());
        verify(companyRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void submitForReviewMovesCompleteOnboardingIntoReview() {
        Fixture fixture = fixture();
        when(userRepository.findById(fixture.user.getId())).thenReturn(Optional.of(fixture.user));
        when(companyDocumentRepository.findByCompanyId(fixture.company.getId())).thenReturn(requiredFleetDocuments(fixture.company));
        when(vehicleRepository.countByCompanyIdAndActiveTrue(fixture.company.getId())).thenReturn(1L);

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
        when(companyDocumentRepository.findByCompanyId(fixture.company.getId())).thenReturn(requiredFleetDocuments(fixture.company));
        when(vehicleRepository.countByCompanyIdAndActiveTrue(fixture.company.getId())).thenReturn(1L);

        onboardingService.submitForReview(new CustomUserDetails(fixture.user));

        verify(companyRepository, never()).save(any());
        verify(userRepository, never()).save(any());
        verify(companyReviewEventService, never()).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    void brokerValidationDoesNotRequireFleetOnlyFields() {
        Fixture fixture = brokerFixture();
        when(userRepository.findById(fixture.user.getId())).thenReturn(Optional.of(fixture.user));
        when(companyDocumentRepository.findByCompanyId(fixture.company.getId())).thenReturn(requiredBrokerDocuments(fixture.company));

        var validation = onboardingService.validate(new CustomUserDetails(fixture.user));

        assertEquals(true, validation.isSubmissionReady());
        assertEquals(false, validation.getMissingFields().contains("dotNumber"));
        assertEquals(false, validation.getMissingFields().contains("vehicleInformation"));
        assertEquals(false, validation.getMissingDocuments().contains(DocumentType.DOT_REGISTRATION));
    }

    @Test
    void fleetValidationRequiresVehicleInformation() {
        Fixture fixture = fixture();
        when(userRepository.findById(fixture.user.getId())).thenReturn(Optional.of(fixture.user));
        when(companyDocumentRepository.findByCompanyId(fixture.company.getId())).thenReturn(requiredFleetDocuments(fixture.company));
        when(vehicleRepository.countByCompanyIdAndActiveTrue(fixture.company.getId())).thenReturn(0L);

        var validation = onboardingService.validate(new CustomUserDetails(fixture.user));

        assertEquals(false, validation.isSubmissionReady());
        assertEquals(true, validation.getMissingFields().contains("vehicleInformation"));
    }

    @Test
    void rejectedDocumentsDoNotSatisfyRequiredDocumentMatrix() {
        Fixture fixture = fixture();
        CompanyDocument rejectedInsurance = document(
                fixture.company,
                DocumentType.CERTIFICATE_OF_INSURANCE
        );
        rejectedInsurance.setReviewStatus(com.fleetmatch.company.documents.entity.DocumentReviewStatus.REJECTED);
        when(userRepository.findById(fixture.user.getId())).thenReturn(Optional.of(fixture.user));
        when(companyDocumentRepository.findByCompanyId(fixture.company.getId())).thenReturn(List.of(
                document(fixture.company, DocumentType.DOT_REGISTRATION),
                document(fixture.company, DocumentType.MC_AUTHORITY),
                rejectedInsurance,
                document(fixture.company, DocumentType.BUSINESS_REGISTRATION)
        ));
        when(vehicleRepository.countByCompanyIdAndActiveTrue(fixture.company.getId())).thenReturn(1L);

        var validation = onboardingService.validate(new CustomUserDetails(fixture.user));

        assertEquals(false, validation.isSubmissionReady());
        assertEquals(true, validation.getMissingDocuments().contains(DocumentType.CERTIFICATE_OF_INSURANCE));
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
        company.setMcNumber("MC-123456");
        company.setDotNumber("DOT-123456");
        company.setEntityType("LLC");
        company.setEin("12-3456789");
        company.setStateOfFormation("TX");
        company.setHeadquarters("Austin, TX");
        company.setPrimaryContact("Ready Owner");
        company.setInsuranceCoverage("$1M");
        company.setOperatingRegions("TX, OK");
        company.setPhone("+15550123");
        company.setFleetSize(5);

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

    private Fixture brokerFixture() {
        Company company = new Company();
        company.setId(UUID.randomUUID());
        company.setLegalName("Ready Broker LLC");
        company.setEmail("ops@broker.test");
        company.setPhone("+15550999");
        company.setType(CompanyType.BROKER);
        company.setVerificationStatus(CompanyVerificationStatus.PENDING);
        company.setCompanyInformationCompleted(true);
        company.setMarketSurveyCompleted(true);
        company.setMcNumber("MC-654321");
        company.setEntityType("LLC");
        company.setEin("98-7654321");
        company.setStateOfFormation("IL");
        company.setHeadquarters("Chicago, IL");
        company.setPrimaryContact("Broker Owner");
        company.setInsuranceCoverage("$1M");
        company.setOperatingRegions("IL, IN, WI");
        company.setAuthorityStatus("ACTIVE");
        company.setBrokerBondOrTrust("BMC-84");

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setFirstName("Broker");
        user.setLastName("Owner");
        user.setEmail("owner@broker.test");
        user.setPhone("+15550999");
        user.setPassword("encoded-password");
        user.setEmailVerified(true);
        user.setPhoneVerified(true);
        user.setStatus(UserStatus.PHONE_VERIFIED);
        user.setCompany(company);

        return new Fixture(user, company);
    }

    private record Fixture(User user, Company company) {
    }

    private List<CompanyDocument> requiredFleetDocuments(Company company) {
        return List.of(
                document(company, DocumentType.DOT_REGISTRATION),
                document(company, DocumentType.MC_AUTHORITY),
                document(company, DocumentType.CERTIFICATE_OF_INSURANCE),
                document(company, DocumentType.BUSINESS_REGISTRATION)
        );
    }

    private List<CompanyDocument> requiredBrokerDocuments(Company company) {
        return List.of(
                document(company, DocumentType.BUSINESS_REGISTRATION),
                document(company, DocumentType.CERTIFICATE_OF_INSURANCE),
                document(company, DocumentType.MC_AUTHORITY)
        );
    }

    private CompanyDocument document(Company company, DocumentType type) {
        CompanyDocument document = new CompanyDocument();
        document.setId(UUID.randomUUID());
        document.setCompany(company);
        document.setDocumentType(type);
        document.setFileName(type.name() + ".pdf");
        document.setFileUrl("https://docs.test/" + type.name());
        return document;
    }
}
