package com.fleetmatch.company.document.controller;

import com.fleetmatch.security.user.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.fleetmatch.company.document.dto.CompanyDocumentResponse;
import com.fleetmatch.company.document.dto.CreateCompanyDocumentRequest;
import com.fleetmatch.company.document.service.CompanyDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/company/documents")
@RequiredArgsConstructor
public class CompanyDocumentController {

    private final CompanyDocumentService companyDocumentService;

    @PostMapping("/{companyId}")
    public CompanyDocumentResponse createDocument(
            @PathVariable UUID companyId,
            @Valid @RequestBody CreateCompanyDocumentRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return companyDocumentService.createDocument(
                companyId,
                request,
                currentUser
        );
    }
}