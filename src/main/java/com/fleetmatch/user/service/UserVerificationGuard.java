package com.fleetmatch.user.service;

import com.fleetmatch.common.exception.BusinessRuleException;
import com.fleetmatch.company.entity.CompanyVerificationStatus;
import com.fleetmatch.user.entity.PlatformRole;
import com.fleetmatch.user.entity.User;
import com.fleetmatch.user.entity.UserStatus;
import org.springframework.stereotype.Service;

@Service
public class UserVerificationGuard {

    public void requireVerifiedForCoreWorkflow(User user) {
        requireMarketplaceAccess(user);
    }

    public void requireMarketplaceAccess(User user) {
        if (user.getPlatformRole() == PlatformRole.ADMIN) {
            return;
        }

        if (!user.isEmailVerified()) {
            throw new BusinessRuleException("Email must be verified before using core workflow");
        }

        if (user.getPhone() != null &&
                !user.getPhone().isBlank() &&
                !user.isPhoneVerified()) {
            throw new BusinessRuleException("Phone must be verified before using core workflow");
        }

        if (user.getCompany() == null ||
                user.getCompany().getVerificationStatus() != CompanyVerificationStatus.APPROVED) {
            throw new BusinessRuleException("Company must be approved before accessing the marketplace");
        }

        if (user.getStatus() != UserStatus.APPROVED &&
                user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessRuleException("Account must be approved before accessing the marketplace");
        }
    }
}
