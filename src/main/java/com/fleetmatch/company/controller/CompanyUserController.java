package com.fleetmatch.company.controller;

import com.fleetmatch.company.dto.CompanyUserResponse;
import com.fleetmatch.company.dto.CreateCompanyUserRequest;
import com.fleetmatch.company.dto.UpdateCompanyUserRoleRequest;
import com.fleetmatch.company.service.CompanyUserService;
import com.fleetmatch.security.user.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/company/users")
public class CompanyUserController {

    private final CompanyUserService companyUserService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void createCompanyUser(
            @Valid @RequestBody CreateCompanyUserRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {

        companyUserService.createCompanyUser(
                request,
                currentUser
        );
    }

    @GetMapping
    public List<CompanyUserResponse> getCompanyUsers(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {

        return companyUserService.getCompanyUsers(
                currentUser
        );
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCompanyUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {

        companyUserService.deleteCompanyUser(
                userId,
                currentUser
        );
    }

    @PutMapping("/{userId}/role")
    public void updateCompanyUserRole(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateCompanyUserRoleRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {

        companyUserService.updateCompanyUserRole(
                userId,
                request,
                currentUser
        );
    }
}