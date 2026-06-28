package com.fleetmatch.company.document.service;

import com.fleetmatch.common.exception.ResourceNotFoundException;
import com.fleetmatch.company.document.dto.CompanyDocumentResponse;
import com.fleetmatch.company.document.dto.CreateCompanyDocumentRequest;
import com.fleetmatch.company.document.entity.CompanyDocument;
import com.fleetmatch.company.document.repository.CompanyDocumentRepository;
import com.fleetmatch.company.entity.Company;
import com.fleetmatch.company.repository.CompanyRepository;
import com.fleetmatch.security.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.fleetmatch.user.entity.PlatformRole;
import com.fleetmatch.user.entity.User;
import com.fleetmatch.user.entity.UserStatus;
import com.fleetmatch.user.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyDocumentService {

    private final CompanyDocumentRepository companyDocumentRepository;

    private final CompanyRepository companyRepository;

    private final UserRepository userRepository;

    public List<CompanyDocumentResponse> getCompanyDocuments(
            UUID companyId
    ) {

        return companyDocumentRepository.findByCompanyId(companyId)
                .stream()
                .map(document -> new CompanyDocumentResponse(
                        document.getId(),
                        document.getDocumentType(),
                        document.getFileName(),
                        document.getFileUrl(),
                        document.getUploadedAt()
                ))
                .toList();
    }

    public CompanyDocumentResponse createDocument(
            UUID companyId,
            CreateCompanyDocumentRequest request,
            CustomUserDetails currentUser
    ) {

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        if (user.getPlatformRole() != PlatformRole.ADMIN) {

            if (user.getCompany() == null ||
                    !user.getCompany().getId().equals(company.getId())) {

                throw new AccessDeniedException(
                        "You can only upload documents for your own company"
                );
            }
        }

        companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        CompanyDocument document = new CompanyDocument();

        document.setCompany(company);
        document.setDocumentType(request.getDocumentType());
        document.setFileName(request.getFileName());
        document.setFileUrl(request.getFileUrl());

        CompanyDocument saved = companyDocumentRepository.save(document);

        if (user.getPlatformRole() != PlatformRole.ADMIN &&
                user.getStatus() != UserStatus.APPROVED &&
                user.getStatus() != UserStatus.ACTIVE &&
                user.getStatus() != UserStatus.SUSPENDED) {
            user.setStatus(UserStatus.DOCUMENTS_PENDING);
            userRepository.save(user);
        }

        return new CompanyDocumentResponse(
                saved.getId(),
                saved.getDocumentType(),
                saved.getFileName(),
                saved.getFileUrl(),
                saved.getUploadedAt()
        );
    }
}
