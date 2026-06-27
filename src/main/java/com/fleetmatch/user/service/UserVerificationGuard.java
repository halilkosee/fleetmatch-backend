package com.fleetmatch.user.service;

import com.fleetmatch.common.exception.BusinessRuleException;
import com.fleetmatch.user.entity.PlatformRole;
import com.fleetmatch.user.entity.User;
import org.springframework.stereotype.Service;

@Service
public class UserVerificationGuard {

    public void requireVerifiedForCoreWorkflow(User user) {
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
    }
}
