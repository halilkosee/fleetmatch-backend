package com.fleetmatch;

import com.fleetmatch.common.exception.BusinessRuleException;
import com.fleetmatch.company.documents.repository.CompanyDocumentRepository;
import com.fleetmatch.company.documents.service.CompanyDocumentService;
import com.fleetmatch.company.documents.service.DocumentStorageService;
import com.fleetmatch.company.dto.UpdateCompanyProfileRequest;
import com.fleetmatch.company.entity.Company;
import com.fleetmatch.company.entity.CompanyType;
import com.fleetmatch.company.entity.CompanyVerificationStatus;
import com.fleetmatch.company.repository.CompanyRepository;
import com.fleetmatch.company.service.CompanyService;
import com.fleetmatch.onboarding.dto.MarketSurveyRequest;
import com.fleetmatch.onboarding.repository.MarketSurveyRepository;
import com.fleetmatch.onboarding.service.OnboardingService;
import com.fleetmatch.security.user.CustomUserDetails;
import com.fleetmatch.user.entity.CompanyUserRole;
import com.fleetmatch.user.entity.PlatformRole;
import com.fleetmatch.user.entity.User;
import com.fleetmatch.user.entity.UserStatus;
import com.fleetmatch.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

class OnboardingReviewLockTest {

    @Test
    void companyProfileCannotBeUpdatedDuringAdminReview() {
        Fixture fixture = fixture();
        CompanyService service = new CompanyService(
                companyRepository(fixture.company),
                userRepository(fixture.user),
                null,
                null,
                null,
                null
        );

        assertThrows(
                BusinessRuleException.class,
                () -> service.updateProfile(new CustomUserDetails(fixture.user), new UpdateCompanyProfileRequest())
        );
    }

    @Test
    void documentsCannotBeUploadedDuringAdminReview() {
        Fixture fixture = fixture();
        CompanyDocumentService service = new CompanyDocumentService(
                companyDocumentRepository(),
                companyRepository(fixture.company),
                userRepository(fixture.user),
                (DocumentStorageService) null
        );

        assertThrows(
                BusinessRuleException.class,
                () -> service.uploadDocument(
                        fixture.company.getId(),
                        null,
                        new MockMultipartFile("file", "w9.pdf", "application/pdf", new byte[]{1}),
                        new CustomUserDetails(fixture.user)
                )
        );
    }

    @Test
    void marketSurveyCannotBeSubmittedDuringAdminReview() {
        Fixture fixture = fixture();
        OnboardingService service = new OnboardingService(
                userRepository(fixture.user),
                companyRepository(fixture.company),
                companyDocumentRepository(),
                marketSurveyRepository(),
                null
        );

        assertThrows(
                BusinessRuleException.class,
                () -> service.submitSurvey(new MarketSurveyRequest(), new CustomUserDetails(fixture.user))
        );
    }

    private Fixture fixture() {
        UUID companyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Company company = new Company();
        company.setId(companyId);
        company.setLegalName("Locked Logistics LLC");
        company.setEmail("ops@locked.test");
        company.setType(CompanyType.BROKER);
        company.setVerificationStatus(CompanyVerificationStatus.UNDER_REVIEW);

        User user = new User();
        user.setId(userId);
        user.setFirstName("Ops");
        user.setLastName("Owner");
        user.setEmail("owner@locked.test");
        user.setPassword("password");
        user.setPlatformRole(PlatformRole.USER);
        user.setCompanyUserRole(CompanyUserRole.OWNER);
        user.setStatus(UserStatus.IN_REVIEW);
        user.setCompany(company);

        return new Fixture(user, company);
    }

    private UserRepository userRepository(User user) {
        return (UserRepository) Proxy.newProxyInstance(
                UserRepository.class.getClassLoader(),
                new Class<?>[]{UserRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        return Optional.of(user);
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private CompanyRepository companyRepository(Company company) {
        return (CompanyRepository) Proxy.newProxyInstance(
                CompanyRepository.class.getClassLoader(),
                new Class<?>[]{CompanyRepository.class},
                (proxy, method, args) -> {
                    if ("findById".equals(method.getName())) {
                        return Optional.of(company);
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private CompanyDocumentRepository companyDocumentRepository() {
        return (CompanyDocumentRepository) Proxy.newProxyInstance(
                CompanyDocumentRepository.class.getClassLoader(),
                new Class<?>[]{CompanyDocumentRepository.class},
                (proxy, method, args) -> {
                    throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private MarketSurveyRepository marketSurveyRepository() {
        return (MarketSurveyRepository) Proxy.newProxyInstance(
                MarketSurveyRepository.class.getClassLoader(),
                new Class<?>[]{MarketSurveyRepository.class},
                (proxy, method, args) -> {
                    throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private record Fixture(User user, Company company) {
    }
}
