package com.fleetmatch.company.controller;

import com.fleetmatch.company.dto.CompanyProfileResponse;
import com.fleetmatch.company.dto.UpdateCompanySettingsRequest;
import com.fleetmatch.company.service.CompanyService;
import com.fleetmatch.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
@Tag(name = "Company Settings")
@SecurityRequirement(name = "bearerAuth")
public class CompanySettingsController {

    private final CompanyService companyService;

    @GetMapping("/me")
    @Operation(summary = "Get authenticated user's company")
    public CompanyProfileResponse getMyCompany(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return companyService.getMyCompany(currentUser);
    }

    @PutMapping("/me/settings")
    @Operation(summary = "Update company self-service settings")
    public CompanyProfileResponse updateSettings(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody UpdateCompanySettingsRequest request
    ) {
        return companyService.updateSettings(currentUser, request);
    }
}
