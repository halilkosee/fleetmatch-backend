package com.fleetmatch.company.document.service;

import com.fleetmatch.common.exception.ResourceNotFoundException;
import com.fleetmatch.company.document.dto.CompanyDocumentResponse;
import com.fleetmatch.company.document.dto.CreateCompanyDocumentRequest;
import com.fleetmatch.company.document.entity.CompanyDocument;
import com.fleetmatch.company.document.entity.DocumentReviewStatus;
import com.fleetmatch.company.document.entity.DocumentType;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyDocumentService {

    private final CompanyDocumentRepository companyDocumentRepository;

    private final CompanyRepository companyRepository;

    private final UserRepository userRepository;

    private final DocumentStorageService documentStorageService;

    public List<CompanyDocumentResponse> getCompanyDocuments(
            UUID companyId
    ) {

        return companyDocumentRepository.findByCompanyId(companyId)
                .stream()
                .map(this::toResponse)
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

        CompanyDocument document = new CompanyDocument();

        document.setCompany(company);
        document.setDocumentType(request.getDocumentType());
        document.setFileName(request.getFileName());
        document.setFileUrl(request.getFileUrl());
        document.setOriginalFileName(request.getFileName());

        CompanyDocument saved = companyDocumentRepository.save(document);

        if (user.getPlatformRole() != PlatformRole.ADMIN &&
                user.getStatus() != UserStatus.APPROVED &&
                user.getStatus() != UserStatus.ACTIVE &&
                user.getStatus() != UserStatus.SUSPENDED) {
            user.setStatus(UserStatus.DOCUMENTS_PENDING);
            userRepository.save(user);
        }

        return toResponse(saved);
    }

    public CompanyDocumentResponse uploadDocument(
            UUID companyId,
            DocumentType documentType,
            MultipartFile file,
            CustomUserDetails currentUser
    ) {

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        ensureCanAccessCompany(user, company);

        CompanyDocument document = new CompanyDocument();
        document.setCompany(company);
        document.setDocumentType(documentType);
        document.setFileName(file.getOriginalFilename() == null
                ? "document"
                : file.getOriginalFilename());
        document.setFileUrl("pending");
        document.setReviewStatus(DocumentReviewStatus.PENDING);

        document = companyDocumentRepository.save(document);

        StoredDocument storedDocument =
                documentStorageService.store(company.getId(), document.getId(), file);

        document.setFileName(storedDocument.fileName());
        document.setFileUrl(storedDocument.fileUrl());
        document.setStorageKey(storedDocument.storageKey());
        document.setOriginalFileName(storedDocument.originalFileName());
        document.setContentType(storedDocument.contentType());
        document.setFileSizeBytes(storedDocument.fileSizeBytes());
        document.setReviewStatus(DocumentReviewStatus.PENDING);
        document.setReviewNotes(null);
        document.setReviewedAt(null);
        document.setReviewedByUserId(null);

        CompanyDocument saved = companyDocumentRepository.save(document);
        advanceUserAfterDocumentUpload(user);

        return toResponse(saved);
    }

    public DocumentDownload downloadDocument(
            UUID documentId,
            CustomUserDetails currentUser
    ) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        CompanyDocument document = companyDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Company document not found"));

        ensureCanAccessCompany(user, document.getCompany());

        if (document.getStorageKey() == null || document.getStorageKey().isBlank()) {
            throw new ResourceNotFoundException("Stored document file not found");
        }

        return documentStorageService.load(
                document.getStorageKey(),
                document.getOriginalFileName() == null
                        ? document.getFileName()
                        : document.getOriginalFileName(),
                document.getContentType()
        );
    }

    private void ensureCanAccessCompany(User user, Company company) {
        if (user.getPlatformRole() == PlatformRole.ADMIN) {
            return;
        }

        if (user.getCompany() == null ||
                !user.getCompany().getId().equals(company.getId())) {
            throw new AccessDeniedException(
                    "You can only access documents for your own company"
            );
        }
    }

    private void advanceUserAfterDocumentUpload(User user) {
        if (user.getPlatformRole() != PlatformRole.ADMIN &&
                user.getStatus() != UserStatus.APPROVED &&
                user.getStatus() != UserStatus.ACTIVE &&
                user.getStatus() != UserStatus.SUSPENDED) {
            user.setStatus(UserStatus.DOCUMENTS_PENDING);
            userRepository.save(user);
        }
    }

    private CompanyDocumentResponse toResponse(CompanyDocument document) {
        return new CompanyDocumentResponse(
                document.getId(),
                document.getDocumentType(),
                document.getFileName(),
                document.getFileUrl(),
                document.getOriginalFileName(),
                document.getContentType(),
                document.getFileSizeBytes(),
                document.getReviewStatus(),
                document.getReviewNotes(),
                document.getReviewedAt(),
                document.getUploadedAt()
        );
    }
}
