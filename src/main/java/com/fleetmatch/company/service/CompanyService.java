package com.fleetmatch.company.service;

import com.fleetmatch.common.exception.ResourceNotFoundException;
import com.fleetmatch.company.dto.CompanyProfileResponse;
import com.fleetmatch.company.dto.PendingCompanyResponse;
import com.fleetmatch.company.dto.UpdateCompanyProfileRequest;
import com.fleetmatch.company.entity.Company;
import com.fleetmatch.company.entity.CompanyVerificationStatus;
import com.fleetmatch.company.repository.CompanyRepository;
import com.fleetmatch.security.user.CustomUserDetails;
import com.fleetmatch.user.entity.User;
import com.fleetmatch.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.fleetmatch.company.dto.CompanyResponse;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;

    private final UserRepository userRepository;

    public void verifyCompany(UUID companyId) {
        approveCompany(companyId);
    }

    public void approveCompany(UUID companyId) {

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        company.setVerificationStatus(
                CompanyVerificationStatus.APPROVED
        );

        companyRepository.save(company);
    }

    public void rejectCompany(UUID companyId) {

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        company.setVerificationStatus(
                CompanyVerificationStatus.REJECTED
        );

        companyRepository.save(company);
    }

    public List<PendingCompanyResponse> getPendingCompanies() {

        return companyRepository.findByVerificationStatusIn(
                        List.of(
                                CompanyVerificationStatus.PENDING,
                                CompanyVerificationStatus.UNDER_REVIEW
                        )
                )
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
                company.getWebsite()
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
                saved.getWebsite()
        );
    }
}
