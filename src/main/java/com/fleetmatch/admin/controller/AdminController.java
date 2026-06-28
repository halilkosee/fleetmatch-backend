package com.fleetmatch.admin.controller;

import com.fleetmatch.admin.dto.AdminCompanyStatusRequest;
import com.fleetmatch.admin.dto.AdminDashboardResponse;
import com.fleetmatch.admin.dto.AdminLoadResponse;
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
import com.fleetmatch.load.dto.LoadResponse;
import com.fleetmatch.load.entity.LoadStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.fleetmatch.security.user.CustomUserDetails;

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
    public String approveUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        adminService.approveUser(userId, currentUser);
        return "User approved";
    }

    @PutMapping("/users/{userId}/suspend")
    public String suspendUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        adminService.suspendUser(userId, currentUser);
        return "User suspended";
    }

    @PutMapping("/users/{userId}/unlock")
    public String unlockUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        adminService.unlockUser(userId, currentUser);
        return "User unlocked";
    }

    @GetMapping("/users/pending")
    public List<PendingUserResponse> getPendingUsers() {
        return adminService.getPendingUsers();
    }

    @GetMapping("/dashboard")
    public AdminDashboardResponse getDashboard() {
        return adminService.getDashboard();
    }

    @GetMapping("/loads")
    public Page<AdminLoadResponse> getLoads(
            @RequestParam(required = false) LoadStatus status,
            @RequestParam(required = false) UUID brokerCompanyId,
            @RequestParam(required = false) String keyword,
            Pageable pageable
    ) {
        return adminService.getLoads(
                status,
                brokerCompanyId,
                keyword,
                pageable
        );
    }

    @GetMapping("/loads/{loadId}")
    public AdminLoadResponse getLoad(
            @PathVariable UUID loadId
    ) {
        return adminService.getLoad(loadId);
    }

    @PatchMapping("/loads/{loadId}/cancel")
    public LoadResponse cancelLoad(
            @PathVariable UUID loadId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return adminService.cancelLoad(loadId, currentUser);
    }

    @PutMapping("/companies/{companyId}/verify")
    public String verifyCompany(
            @PathVariable UUID companyId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        companyService.verifyCompany(companyId, currentUser);
        return "Company verified";
    }

    @PatchMapping("/companies/{companyId}/approve")
    public String approveCompany(
            @PathVariable UUID companyId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        companyService.verifyCompany(companyId, currentUser);
        return "Company approved";
    }

    @PutMapping("/companies/{companyId}/reject")
    public String rejectCompany(
            @PathVariable UUID companyId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        companyService.rejectCompany(companyId, currentUser);
        return "Company rejected";
    }

    @PatchMapping("/companies/{companyId}/reject")
    public String patchRejectCompany(
            @PathVariable UUID companyId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        companyService.rejectCompany(companyId, currentUser);
        return "Company rejected";
    }

    @PatchMapping("/companies/{companyId}/suspend")
    public String suspendCompany(
            @PathVariable UUID companyId,
            @Valid @RequestBody(required = false) AdminCompanyStatusRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        companyService.suspendCompany(
                companyId,
                request == null ? null : request.getNotes(),
                currentUser
        );
        return "Company suspended";
    }

    @PatchMapping("/companies/{companyId}/reactivate")
    public String reactivateCompany(
            @PathVariable UUID companyId,
            @Valid @RequestBody(required = false) AdminCompanyStatusRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        companyService.reactivateCompany(
                companyId,
                request == null ? null : request.getNotes(),
                currentUser
        );
        return "Company reactivated";
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
            @RequestParam(required = false) UUID entityId,
            @RequestParam(required = false) String actorEmail,
            @RequestParam(required = false) UUID actorCompanyId,
            @RequestParam(required = false) LocalDateTime from,
            @RequestParam(required = false) LocalDateTime to,
            Pageable pageable
    ) {
        return auditLogService.search(
                action,
                entityType,
                entityId,
                actorEmail,
                actorCompanyId,
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
