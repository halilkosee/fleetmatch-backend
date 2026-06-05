package com.fleetmatch.company.controller;

import com.fleetmatch.company.dto.CompanyProfileResponse;
import com.fleetmatch.company.dto.UpdateCompanyProfileRequest;
import com.fleetmatch.company.service.CompanyService;
import com.fleetmatch.security.user.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/company")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @GetMapping("/profile")
    public CompanyProfileResponse getProfile(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return companyService.getProfile(currentUser);
    }

    @PutMapping("/profile")
    public CompanyProfileResponse updateProfile(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody UpdateCompanyProfileRequest request
    ) {
        return companyService.updateProfile(
                currentUser,
                request
        );
    }
}