package com.fleetmatch.company.service;

import com.fleetmatch.company.dto.CompanyUserResponse;
import com.fleetmatch.company.dto.CreateCompanyUserRequest;
import com.fleetmatch.company.dto.UpdateCompanyUserRoleRequest;
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

        userRepository.save(user);
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
                        .build())
                .toList();
    }

    public void deleteCompanyUser(
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

        User userToDelete = userRepository.findById(
                userId
        ).orElseThrow();

        if (!userToDelete.getCompany().getId().equals(
                owner.getCompany().getId()
        )) {
            throw new AccessDeniedException(
                    "User does not belong to your company"
            );
        }

        if (userToDelete.getCompanyUserRole() == CompanyUserRole.OWNER) {
            throw new IllegalArgumentException(
                    "Owner cannot be deleted"
            );
        }

        if (userToDelete.getId().equals(owner.getId())) {
            throw new IllegalArgumentException(
                    "You cannot delete yourself"
            );
        }

        userRepository.delete(userToDelete);
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

        user.setCompanyUserRole(
                request.getCompanyUserRole()
        );

        userRepository.save(user);
    }
}