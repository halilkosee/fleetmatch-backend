package com.fleetmatch.company.service;

import com.fleetmatch.audit.service.AuditLogService;
import com.fleetmatch.company.entity.Company;
import com.fleetmatch.company.entity.CompanyType;
import com.fleetmatch.company.entity.CompanyVerificationStatus;
import com.fleetmatch.company.repository.CompanyRepository;
import com.fleetmatch.company.review.service.CompanyReviewEventService;
import com.fleetmatch.email.service.EmailTemplateService;
import com.fleetmatch.notification.inapp.service.NotificationService;
import com.fleetmatch.security.user.CustomUserDetails;
import com.fleetmatch.user.entity.PlatformRole;
import com.fleetmatch.user.entity.User;
import com.fleetmatch.user.entity.UserStatus;
import com.fleetmatch.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyAdminReviewServiceTest {

    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private EmailTemplateService emailTemplateService;
    @Mock
    private CompanyReviewEventService companyReviewEventService;

    @InjectMocks
    private CompanyService companyService;

    @Test
    void approveCompanyApprovesNonSuspendedCompanyUsers() {
        Fixture fixture = fixture();
        when(companyRepository.findById(fixture.company.getId())).thenReturn(Optional.of(fixture.company));
        when(userRepository.findByCompanyId(fixture.company.getId())).thenReturn(List.of(fixture.owner));
        when(userRepository.findById(fixture.admin.getId())).thenReturn(Optional.of(fixture.admin));

        companyService.verifyCompany(fixture.company.getId(), new CustomUserDetails(fixture.admin));

        assertEquals(CompanyVerificationStatus.APPROVED, fixture.company.getVerificationStatus());
        assertEquals(UserStatus.APPROVED, fixture.owner.getStatus());
        verify(companyRepository).save(fixture.company);
        verify(userRepository).saveAll(List.of(fixture.owner));
        verify(companyReviewEventService).record(any(), any(), any(), any(), any(), any());
    }

    @Test
    void rejectCompanyStoresReasonAndRejectsCompanyUsers() {
        Fixture fixture = fixture();
        when(companyRepository.findById(fixture.company.getId())).thenReturn(Optional.of(fixture.company));
        when(userRepository.findByCompanyId(fixture.company.getId())).thenReturn(List.of(fixture.owner));
        when(userRepository.findById(fixture.admin.getId())).thenReturn(Optional.of(fixture.admin));

        companyService.rejectCompany(
                fixture.company.getId(),
                "Insurance mismatch",
                "COI does not match legal entity",
                new CustomUserDetails(fixture.admin)
        );

        assertEquals(CompanyVerificationStatus.REJECTED, fixture.company.getVerificationStatus());
        assertEquals("Insurance mismatch", fixture.company.getRejectionReason());
        assertEquals(UserStatus.REJECTED, fixture.owner.getStatus());
        verify(userRepository).saveAll(List.of(fixture.owner));
    }

    @Test
    void suspendCompanyBlocksMarketplaceWithoutChangingUsers() {
        Fixture fixture = fixture();
        fixture.company.setVerificationStatus(CompanyVerificationStatus.APPROVED);
        fixture.owner.setStatus(UserStatus.ACTIVE);
        when(companyRepository.findById(fixture.company.getId())).thenReturn(Optional.of(fixture.company));
        when(userRepository.findById(fixture.admin.getId())).thenReturn(Optional.of(fixture.admin));

        companyService.suspendCompany(
                fixture.company.getId(),
                "Compliance hold",
                new CustomUserDetails(fixture.admin)
        );

        assertEquals(CompanyVerificationStatus.SUSPENDED, fixture.company.getVerificationStatus());
        assertEquals("Compliance hold", fixture.company.getVerificationNotes());
        assertEquals(UserStatus.ACTIVE, fixture.owner.getStatus());
        verify(companyRepository).save(fixture.company);
    }

    private Fixture fixture() {
        Company company = new Company();
        company.setId(UUID.randomUUID());
        company.setLegalName("Review Fleet LLC");
        company.setEmail("ops@review.test");
        company.setType(CompanyType.FLEET);
        company.setVerificationStatus(CompanyVerificationStatus.UNDER_REVIEW);

        User owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setFirstName("Fleet");
        owner.setLastName("Owner");
        owner.setEmail("owner@review.test");
        owner.setPassword("encoded");
        owner.setStatus(UserStatus.IN_REVIEW);
        owner.setCompany(company);

        User admin = new User();
        admin.setId(UUID.randomUUID());
        admin.setFirstName("Admin");
        admin.setLastName("User");
        admin.setEmail("admin@easyfleetmatch.test");
        admin.setPassword("encoded");
        admin.setPlatformRole(PlatformRole.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);

        return new Fixture(company, owner, admin);
    }

    private record Fixture(Company company, User owner, User admin) {
    }
}
