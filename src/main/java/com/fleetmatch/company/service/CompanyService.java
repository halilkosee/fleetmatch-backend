package com.fleetmatch.company.service;

import com.fleetmatch.common.exception.ResourceNotFoundException;
import com.fleetmatch.audit.entity.AuditAction;
import com.fleetmatch.audit.service.AuditLogService;
import com.fleetmatch.company.dto.CompanyProfileResponse;
import com.fleetmatch.company.dto.PendingCompanyResponse;
import com.fleetmatch.company.dto.UpdateCompanyProfileRequest;
import com.fleetmatch.company.dto.UpdateCompanySettingsRequest;
import com.fleetmatch.company.entity.Company;
import com.fleetmatch.company.entity.CompanyVerificationStatus;
import com.fleetmatch.company.repository.CompanyRepository;
import com.fleetmatch.security.user.CustomUserDetails;
import com.fleetmatch.user.entity.User;
import com.fleetmatch.user.entity.CompanyUserRole;
import com.fleetmatch.user.repository.UserRepository;
import com.fleetmatch.notification.entity.NotificationType;
import com.fleetmatch.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fleetmatch.company.dto.CompanyResponse;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public void verifyCompany(UUID companyId) {

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        company.setVerificationStatus(
                CompanyVerificationStatus.APPROVED
        );

        companyRepository.save(company);
        notificationService.createForCompany(
                company,
                NotificationType.COMPANY_APPROVED,
                "Company approved",
                "Your company has been approved",
                "COMPANY",
                company.getId()
        );
        auditLogService.log(null, AuditAction.COMPANY_APPROVED, "COMPANY", company.getId(), "Company approved");
    }

    public void rejectCompany(UUID companyId) {

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        company.setVerificationStatus(
                CompanyVerificationStatus.REJECTED
        );

        companyRepository.save(company);
        notificationService.createForCompany(
                company,
                NotificationType.COMPANY_REJECTED,
                "Company rejected",
                "Your company verification was rejected",
                "COMPANY",
                company.getId()
        );
        auditLogService.log(null, AuditAction.COMPANY_REJECTED, "COMPANY", company.getId(), "Company rejected");
    }

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
                company.getPhone(),
                company.getWebsite(),
                company.getDbaName(),
                company.getEmail(),
                company.getFleetSize(),
                company.getDescription()
        );
    }

    public CompanyProfileResponse updateProfile(
            CustomUserDetails currentUser,
            UpdateCompanyProfileRequest request
    ) {

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Company company = user.getCompany();

        company.setMcNumber(request.getMcNumber());
        company.setDotNumber(request.getDotNumber());
        company.setPhone(request.getPhone());
        company.setWebsite(request.getWebsite());

        Company saved = companyRepository.save(company);

        return new CompanyProfileResponse(
                saved.getId(),
                saved.getLegalName(),
                saved.getType(),
                saved.getVerificationStatus(),
                saved.getMcNumber(),
                saved.getDotNumber(),
                saved.getPhone(),
                saved.getWebsite(),
                saved.getDbaName(),
                saved.getEmail(),
                saved.getFleetSize(),
                saved.getDescription()
        );
    }

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
        company.setDbaName(request.getDbaName());
        company.setWebsite(request.getWebsite());
        company.setEmail(request.getCompanyEmail());
        company.setPhone(request.getCompanyPhone());
        company.setFleetSize(request.getFleetSize());
        company.setDescription(request.getDescription());

        Company saved = companyRepository.save(company);
        auditLogService.log(user, AuditAction.COMPANY_UPDATED, "COMPANY", saved.getId(), "Company settings updated");
        return toCompanyProfile(saved);
    }

    private CompanyProfileResponse toCompanyProfile(Company company) {
        return new CompanyProfileResponse(
                company.getId(),
                company.getLegalName(),
                company.getType(),
                company.getVerificationStatus(),
                company.getMcNumber(),
                company.getDotNumber(),
                company.getPhone(),
                company.getWebsite(),
                company.getDbaName(),
                company.getEmail(),
                company.getFleetSize(),
                company.getDescription()
        );
    }
}
