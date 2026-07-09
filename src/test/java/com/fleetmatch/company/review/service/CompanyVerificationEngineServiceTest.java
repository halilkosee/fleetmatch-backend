package com.fleetmatch.company.review.service;

import com.fleetmatch.common.exception.BusinessRuleException;
import com.fleetmatch.company.entity.Company;
import com.fleetmatch.company.entity.CompanyType;
import com.fleetmatch.company.entity.CompanyVerificationStatus;
import com.fleetmatch.company.repository.CompanyRepository;
import com.fleetmatch.company.review.entity.CompanyVerificationChecklistItem;
import com.fleetmatch.company.review.entity.CompanyVerificationRiskAssessment;
import com.fleetmatch.company.review.entity.CompanyVerificationSnapshot;
import com.fleetmatch.company.review.entity.VerificationRiskLevel;
import com.fleetmatch.company.review.repository.CompanyVerificationChecklistItemRepository;
import com.fleetmatch.company.review.repository.CompanyVerificationRiskAssessmentRepository;
import com.fleetmatch.company.review.repository.CompanyVerificationSectionReviewRepository;
import com.fleetmatch.company.review.repository.CompanyVerificationSnapshotRepository;
import com.fleetmatch.onboarding.dto.OnboardingValidationResponse;
import com.fleetmatch.user.entity.User;
import com.fleetmatch.user.entity.UserStatus;
import com.fleetmatch.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyVerificationEngineServiceTest {

    @Mock
    private CompanyVerificationSnapshotRepository snapshotRepository;
    @Mock
    private CompanyVerificationRiskAssessmentRepository riskAssessmentRepository;
    @Mock
    private CompanyVerificationChecklistItemRepository checklistItemRepository;
    @Mock
    private CompanyVerificationSectionReviewRepository sectionReviewRepository;
    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private CompanyReviewEventService companyReviewEventService;

    @InjectMocks
    private CompanyVerificationEngineService companyVerificationEngineService;

    @Test
    void createSnapshotBuildsChecklistAndRiskAssessment() {
        Company company = company();
        User user = user(company);
        when(snapshotRepository.findTopByCompanyIdOrderByVersionNumberDesc(company.getId()))
                .thenReturn(Optional.empty());
        when(snapshotRepository.save(any(CompanyVerificationSnapshot.class)))
                .thenAnswer(invocation -> {
                    CompanyVerificationSnapshot snapshot = invocation.getArgument(0);
                    snapshot.setId(UUID.randomUUID());
                    return snapshot;
                });
        when(companyRepository.countDuplicateMcNumber(company.getId(), company.getMcNumber()))
                .thenReturn(1L);
        when(vehicleRepository.countByCompanyIdAndActiveTrue(company.getId())).thenReturn(2L);

        companyVerificationEngineService.createSnapshot(company, user, validation());

        ArgumentCaptor<CompanyVerificationRiskAssessment> riskCaptor =
                ArgumentCaptor.forClass(CompanyVerificationRiskAssessment.class);
        verify(riskAssessmentRepository).save(riskCaptor.capture());
        assertEquals(VerificationRiskLevel.LOW, riskCaptor.getValue().getLevel());
        assertTrue(riskCaptor.getValue().getSignals().contains("Duplicate MC number"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CompanyVerificationChecklistItem>> checklistCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(checklistItemRepository).saveAll(checklistCaptor.capture());
        assertTrue(checklistCaptor.getValue().stream()
                .anyMatch(item -> item.getItemKey().equals("dot_verified")));
    }

    @Test
    void approvalReadinessBlocksIncompleteMandatoryChecklist() {
        Company company = company();
        CompanyVerificationSnapshot snapshot = new CompanyVerificationSnapshot();
        snapshot.setId(UUID.randomUUID());
        snapshot.setSubmissionReady(true);
        snapshot.setVersionNumber(1);
        when(snapshotRepository.findTopByCompanyIdOrderByVersionNumberDesc(company.getId()))
                .thenReturn(Optional.of(snapshot));
        when(riskAssessmentRepository.findTopByCompanyIdOrderByCreatedAtDesc(company.getId()))
                .thenReturn(Optional.of(new CompanyVerificationRiskAssessment()));
        when(checklistItemRepository.countBySnapshotIdAndMandatoryTrueAndCompletedFalse(snapshot.getId()))
                .thenReturn(1L);

        assertThrows(
                BusinessRuleException.class,
                () -> companyVerificationEngineService.validateApprovalReadiness(company)
        );
    }

    private OnboardingValidationResponse validation() {
        return new OnboardingValidationResponse(
                CompanyType.FLEET,
                true,
                100,
                List.of("verification", "company_information", "documents", "survey", "profile"),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                "1-2 business days"
        );
    }

    private Company company() {
        Company company = new Company();
        company.setId(UUID.randomUUID());
        company.setLegalName("Risk Fleet LLC");
        company.setEmail("ops@riskfleet.test");
        company.setPhone("+15550101");
        company.setType(CompanyType.FLEET);
        company.setVerificationStatus(CompanyVerificationStatus.PENDING);
        company.setMcNumber("MC-123456");
        company.setDotNumber("DOT-123456");
        company.setEin("12-3456789");
        company.setFleetSize(2);
        return company;
    }

    private User user(Company company) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("owner@riskfleet.test");
        user.setPassword("encoded");
        user.setStatus(UserStatus.PHONE_VERIFIED);
        user.setCompany(company);
        return user;
    }
}
