package com.fleetmatch.company.service;

import com.fleetmatch.common.exception.ResourceNotFoundException;
import com.fleetmatch.company.dto.PendingCompanyResponse;
import com.fleetmatch.company.entity.Company;
import com.fleetmatch.company.entity.CompanyVerificationStatus;
import com.fleetmatch.company.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.fleetmatch.company.dto.CompanyResponse;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;

    public void verifyCompany(UUID companyId) {

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        company.setVerificationStatus(
                CompanyVerificationStatus.VERIFIED
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

        return companyRepository.findByVerificationStatus(
                        CompanyVerificationStatus.PENDING
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

}