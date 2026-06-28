package com.fleetmatch.onboarding.dto;

import com.fleetmatch.company.entity.CompanyType;
import com.fleetmatch.company.entity.CompanyVerificationStatus;
import com.fleetmatch.user.entity.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OnboardingProgressResponse {

    private UserStatus userStatus;
    private CompanyType companyType;
    private CompanyVerificationStatus companyVerificationStatus;
    private boolean emailVerified;
    private boolean phoneVerified;
    private boolean companyInformationCompleted;
    private boolean documentsUploaded;
    private boolean marketSurveyCompleted;
    private boolean inReview;
    private boolean approved;
    private String estimatedReviewTime;
    private String message;
}
