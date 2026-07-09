package com.fleetmatch.admin.service;

import com.fleetmatch.admin.dto.AdminReviewActionRequest;
import com.fleetmatch.audit.service.AuditLogService;
import com.fleetmatch.company.documents.dto.ReviewCompanyDocumentRequest;
import com.fleetmatch.company.documents.entity.CompanyDocument;
import com.fleetmatch.company.documents.entity.DocumentReviewStatus;
import com.fleetmatch.company.documents.entity.DocumentType;
import com.fleetmatch.company.documents.repository.CompanyDocumentRepository;
import com.fleetmatch.company.entity.Company;
import com.fleetmatch.company.entity.CompanyType;
import com.fleetmatch.company.entity.CompanyVerificationStatus;
import com.fleetmatch.company.repository.CompanyRepository;
import com.fleetmatch.company.review.service.CompanyReviewEventService;
import com.fleetmatch.company.review.service.CompanyVerificationEngineService;
import com.fleetmatch.email.service.EmailTemplateService;
import com.fleetmatch.load.repository.LoadRepository;
import com.fleetmatch.load.service.LoadService;
import com.fleetmatch.messaging.repository.ConversationRepository;
import com.fleetmatch.messaging.repository.MessageRepository;
import com.fleetmatch.messaging.service.MessagingService;
import com.fleetmatch.notification.inapp.service.NotificationService;
import com.fleetmatch.offer.repository.OfferRepository;
import com.fleetmatch.onboarding.repository.MarketSurveyRepository;
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
class AdminOnboardingReviewServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private LoadRepository loadRepository;
    @Mock
    private OfferRepository offerRepository;
    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private CompanyDocumentRepository companyDocumentRepository;
    @Mock
    private MarketSurveyRepository marketSurveyRepository;
    @Mock
    private CompanyReviewEventService companyReviewEventService;
    @Mock
    private CompanyVerificationEngineService companyVerificationEngineService;
    @Mock
    private LoadService loadService;
    @Mock
    private MessagingService messagingService;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private EmailTemplateService emailTemplateService;

    @InjectMocks
    private AdminService adminService;

    @Test
    void requestAdditionalDocumentsUnlocksCompanyUsersForResubmission() {
        Fixture fixture = fixture();
        AdminReviewActionRequest request = new AdminReviewActionRequest();
        request.setReason("Updated insurance certificate is required");
        when(companyRepository.findById(fixture.company.getId())).thenReturn(Optional.of(fixture.company));
        when(userRepository.findByCompanyId(fixture.company.getId())).thenReturn(List.of(fixture.owner));
        when(userRepository.findById(fixture.admin.getId())).thenReturn(Optional.of(fixture.admin));
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));

        adminService.requestAdditionalDocuments(
                fixture.company.getId(),
                request,
                new CustomUserDetails(fixture.admin)
        );

        assertEquals(CompanyVerificationStatus.PENDING, fixture.company.getVerificationStatus());
        assertEquals(UserStatus.DOCUMENTS_PENDING, fixture.owner.getStatus());
        verify(userRepository).saveAll(List.of(fixture.owner));
    }

    @Test
    void rejectedDocumentUnlocksCompanyUsersForResubmission() {
        Fixture fixture = fixture();
        CompanyDocument document = new CompanyDocument();
        document.setId(UUID.randomUUID());
        document.setCompany(fixture.company);
        document.setDocumentType(DocumentType.CERTIFICATE_OF_INSURANCE);
        document.setFileName("insurance.pdf");
        document.setFileUrl("https://docs.test/insurance.pdf");
        ReviewCompanyDocumentRequest request = new ReviewCompanyDocumentRequest();
        request.setReviewStatus(DocumentReviewStatus.REQUESTED_AGAIN);
        request.setReviewNotes("Certificate holder is missing");
        when(companyRepository.findById(fixture.company.getId())).thenReturn(Optional.of(fixture.company));
        when(companyDocumentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(companyDocumentRepository.save(any(CompanyDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findByCompanyId(fixture.company.getId())).thenReturn(List.of(fixture.owner));
        when(userRepository.findById(fixture.admin.getId())).thenReturn(Optional.of(fixture.admin));

        adminService.reviewCompanyDocument(
                fixture.company.getId(),
                document.getId(),
                request,
                new CustomUserDetails(fixture.admin)
        );

        assertEquals(CompanyVerificationStatus.PENDING, fixture.company.getVerificationStatus());
        assertEquals(UserStatus.DOCUMENTS_PENDING, fixture.owner.getStatus());
        verify(userRepository).saveAll(List.of(fixture.owner));
    }

    private Fixture fixture() {
        Company company = new Company();
        company.setId(UUID.randomUUID());
        company.setLegalName("Review Logistics LLC");
        company.setEmail("ops@review.test");
        company.setType(CompanyType.FLEET);
        company.setVerificationStatus(CompanyVerificationStatus.UNDER_REVIEW);

        User owner = new User();
        owner.setId(UUID.randomUUID());
        owner.setEmail("owner@review.test");
        owner.setPassword("encoded");
        owner.setStatus(UserStatus.IN_REVIEW);
        owner.setCompany(company);

        User admin = new User();
        admin.setId(UUID.randomUUID());
        admin.setEmail("admin@easyfleetmatch.test");
        admin.setPassword("encoded");
        admin.setPlatformRole(PlatformRole.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);

        return new Fixture(company, owner, admin);
    }

    private record Fixture(Company company, User owner, User admin) {
    }
}
