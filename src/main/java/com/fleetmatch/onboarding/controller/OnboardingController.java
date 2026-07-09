package com.fleetmatch.onboarding.controller;

import com.fleetmatch.onboarding.dto.MarketSurveyRequest;
import com.fleetmatch.onboarding.dto.OnboardingPreviewResponse;
import com.fleetmatch.onboarding.dto.OnboardingProgressResponse;
import com.fleetmatch.onboarding.dto.OnboardingValidationResponse;
import com.fleetmatch.onboarding.service.OnboardingService;
import com.fleetmatch.security.user.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    @GetMapping("/progress")
    public OnboardingProgressResponse getProgress(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return onboardingService.getProgress(currentUser);
    }

    @GetMapping("/validation")
    public OnboardingValidationResponse validate(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return onboardingService.validate(currentUser);
    }

    @GetMapping("/preview")
    public OnboardingPreviewResponse preview(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return onboardingService.preview(currentUser);
    }

    @PostMapping("/survey")
    public OnboardingProgressResponse submitSurvey(
            @Valid @RequestBody MarketSurveyRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return onboardingService.submitSurvey(request, currentUser);
    }

    @PostMapping("/submit-review")
    public OnboardingProgressResponse submitForReview(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return onboardingService.submitForReview(currentUser);
    }
}
