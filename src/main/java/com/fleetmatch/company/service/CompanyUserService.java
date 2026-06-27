package com.fleetmatch.company.service;

import com.fleetmatch.audit.entity.AuditAction;
import com.fleetmatch.audit.service.AuditLogService;
import com.fleetmatch.company.dto.CompanyUserResponse;
import com.fleetmatch.company.dto.CreateCompanyUserRequest;
import com.fleetmatch.company.dto.UpdateCompanyUserRoleRequest;
import com.fleetmatch.auth.service.PasswordPolicyService;
import com.fleetmatch.security.user.CustomUserDetails;
import com.fleetmatch.subscription.service.SubscriptionValidationService;
import com.fleetmatch.user.entity.CompanyUserRole;
import com.fleetmatch.user.entity.PlatformRole;
import com.fleetmatch.user.entity.User;
import com.fleetmatch.user.entity.UserStatus;
import com.fleetmatch.user.repository.UserRepository;
import com.fleetmatch.common.exception.ResourceAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SubscriptionValidationService subscriptionValidationService;
    private final PasswordPolicyService passwordPolicyService;
    private final AuditLogService auditLogService;

    public void createCompanyUser(
            CreateCompanyUserRequest request,
            CustomUserDetails currentUser
    ) {

        User owner = userRepository.findById(
                currentUser.getId()
        ).orElseThrow();

        if (owner.getCompanyUserRole() != CompanyUserRole.OWNER) {
            throw new AccessDeniedException(
                    "Only owners can create company users"
            );
        }

        if (request.getCompanyUserRole() == CompanyUserRole.OWNER) {
            throw new IllegalArgumentException(
                    "Company already has an owner"
            );
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceAlreadyExistsException(
                    "Email already exists"
            );
        }

        subscriptionValidationService.validateUserLimit(
                owner.getCompany()
        );

        passwordPolicyService.validate(request.getPassword());

        User user = new User();

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(
                passwordEncoder.encode(
                        request.getPassword()
                )
        );

        user.setPlatformRole(PlatformRole.USER);
        user.setStatus(UserStatus.ACTIVE);

        user.setCompany(owner.getCompany());

        user.setCompanyUserRole(
                request.getCompanyUserRole()
        );

        User saved = userRepository.save(user);
        auditLogService.log(
                owner,
                AuditAction.COMPANY_USER_CREATED,
                "USER",
                saved.getId(),
                "Company user created with role " + saved.getCompanyUserRole()
        );
    }

    public List<CompanyUserResponse> getCompanyUsers(
            CustomUserDetails currentUser
    ) {

        User currentUserEntity = userRepository.findById(
                currentUser.getId()
        ).orElseThrow();

        return userRepository.findByCompanyId(
                        currentUserEntity.getCompany().getId()
                )
                .stream()
                .map(user -> CompanyUserResponse.builder()
                        .id(user.getId())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .email(user.getEmail())
                        .companyUserRole(user.getCompanyUserRole())
                        .status(user.getStatus())
                        .build())
                .toList();
    }

    public void deleteCompanyUser(
            UUID userId,
            CustomUserDetails currentUser
    ) {

        deactivateCompanyUser(
                userId,
                currentUser
        );
    }

    public void activateCompanyUser(
            UUID userId,
            CustomUserDetails currentUser
    ) {

        User owner = userRepository.findById(
                currentUser.getId()
        ).orElseThrow();

        if (owner.getCompanyUserRole() != CompanyUserRole.OWNER) {
            throw new AccessDeniedException(
                    "Only owners can delete company users"
            );
        }

        User userToActivate = userRepository.findById(
                userId
        ).orElseThrow();

        if (!userToActivate.getCompany().getId().equals(
                owner.getCompany().getId()
        )) {
            throw new AccessDeniedException(
                    "User does not belong to your company"
            );
        }

        if (userToActivate.getCompanyUserRole() == CompanyUserRole.OWNER) {
            throw new IllegalArgumentException(
                    "Owner status cannot be changed"
            );
        }

        userToActivate.setStatus(UserStatus.ACTIVE);

        User saved = userRepository.save(userToActivate);
        auditLogService.log(
                owner,
                AuditAction.COMPANY_USER_ACTIVATED,
                "USER",
                saved.getId(),
                "Company user activated"
        );
    }

    public void deactivateCompanyUser(
            UUID userId,
            CustomUserDetails currentUser
    ) {

        User owner = userRepository.findById(
                currentUser.getId()
        ).orElseThrow();

        if (owner.getCompanyUserRole() != CompanyUserRole.OWNER) {
            throw new AccessDeniedException(
                    "Only owners can deactivate company users"
            );
        }

        User userToDeactivate = userRepository.findById(
                userId
        ).orElseThrow();

        if (!userToDeactivate.getCompany().getId().equals(
                owner.getCompany().getId()
        )) {
            throw new AccessDeniedException(
                    "User does not belong to your company"
            );
        }

        if (userToDeactivate.getId().equals(owner.getId())) {
            throw new IllegalArgumentException(
                    "You cannot deactivate yourself"
            );
        }

        if (userToDeactivate.getCompanyUserRole() == CompanyUserRole.OWNER) {
            throw new IllegalArgumentException(
                    "Owner cannot be deactivated"
            );
        }

        userToDeactivate.setStatus(UserStatus.SUSPENDED);

        User saved = userRepository.save(userToDeactivate);
        auditLogService.log(
                owner,
                AuditAction.COMPANY_USER_DEACTIVATED,
                "USER",
                saved.getId(),
                "Company user deactivated"
        );
    }

    public void updateCompanyUserRole(
            UUID userId,
            UpdateCompanyUserRoleRequest request,
            CustomUserDetails currentUser
    ) {

        User owner = userRepository.findById(
                currentUser.getId()
        ).orElseThrow();

        if (owner.getCompanyUserRole() != CompanyUserRole.OWNER) {
            throw new AccessDeniedException(
                    "Only owners can update company users"
            );
        }

        User user = userRepository.findById(
                userId
        ).orElseThrow();

        if (!user.getCompany().getId().equals(
                owner.getCompany().getId()
        )) {
            throw new AccessDeniedException(
                    "User does not belong to your company"
            );
        }

        if (user.getCompanyUserRole() == CompanyUserRole.OWNER) {
            throw new IllegalArgumentException(
                    "Owner role cannot be modified"
            );
        }

        if (request.getCompanyUserRole() == CompanyUserRole.OWNER) {
            throw new IllegalArgumentException(
                    "Company already has an owner"
            );
        }

        CompanyUserRole previousRole = user.getCompanyUserRole();

        user.setCompanyUserRole(
                request.getCompanyUserRole()
        );

        User saved = userRepository.save(user);
        auditLogService.log(
                owner,
                AuditAction.COMPANY_USER_ROLE_CHANGED,
                "USER",
                saved.getId(),
                "Company user role changed from " + previousRole + " to " + saved.getCompanyUserRole()
        );
    }
}
