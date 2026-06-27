package com.fleetmatch.admin.controller;

import com.fleetmatch.admin.dto.AdminDashboardResponse;
import com.fleetmatch.admin.dto.PendingUserResponse;
import com.fleetmatch.admin.service.AdminService;
import com.fleetmatch.audit.dto.AuditLogResponse;
import com.fleetmatch.audit.entity.AuditAction;
import com.fleetmatch.audit.service.AuditLogService;
import com.fleetmatch.company.document.dto.CompanyDocumentResponse;
import com.fleetmatch.company.document.service.CompanyDocumentService;
import com.fleetmatch.company.dto.CompanyResponse;
import com.fleetmatch.company.dto.PendingCompanyResponse;
import com.fleetmatch.company.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final CompanyDocumentService companyDocumentService;

    private final AdminService adminService;

    private final CompanyService companyService;

    private final AuditLogService auditLogService;

    @PutMapping("/users/{userId}/approve")
    public String approveUser(@PathVariable UUID userId) {
        adminService.approveUser(userId);
        return "User approved";
    }

    @PutMapping("/users/{userId}/suspend")
    public String suspendUser(@PathVariable UUID userId) {
        adminService.suspendUser(userId);
        return "User suspended";
    }

    @GetMapping("/users/pending")
    public List<PendingUserResponse> getPendingUsers() {
        return adminService.getPendingUsers();
    }

    @GetMapping("/dashboard")
    public AdminDashboardResponse getDashboard() {
        return adminService.getDashboard();
    }

    @PutMapping("/companies/{companyId}/verify")
    public String verifyCompany(
            @PathVariable UUID companyId
    ) {
        companyService.verifyCompany(companyId);
        return "Company verified";
    }

    @PatchMapping("/companies/{companyId}/approve")
    public String approveCompany(
            @PathVariable UUID companyId
    ) {
        companyService.verifyCompany(companyId);
        return "Company approved";
    }

    @PutMapping("/companies/{companyId}/reject")
    public String rejectCompany(
            @PathVariable UUID companyId
    ) {
        companyService.rejectCompany(companyId);
        return "Company rejected";
    }

    @PatchMapping("/companies/{companyId}/reject")
    public String patchRejectCompany(
            @PathVariable UUID companyId
    ) {
        companyService.rejectCompany(companyId);
        return "Company rejected";
    }

    @GetMapping("/companies/pending")
    public List<PendingCompanyResponse> getPendingCompanies() {
        return companyService.getPendingCompanies();
    }

    @GetMapping("/companies/{companyId}/documents")
    public List<CompanyDocumentResponse> getCompanyDocuments(
            @PathVariable UUID companyId
    ) {
        return companyDocumentService.getCompanyDocuments(companyId);
    }

    @GetMapping("/companies")
    public List<CompanyResponse> getAllCompanies() {
        return companyService.getAllCompanies();
    }

    @GetMapping("/audit-logs")
    public Page<AuditLogResponse> getAuditLogs(
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String actorEmail,
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to,
            Pageable pageable
    ) {
        return auditLogService.search(
                action,
                entityType,
                actorEmail,
                from,
                to,
                pageable
        );
    }

    @GetMapping("/companies/{companyId}")
    public CompanyResponse getCompany(
            @PathVariable UUID companyId
    ) {
        return companyService.getCompany(companyId);
    }
}
