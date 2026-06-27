package com.fleetmatch.user.repository;

import com.fleetmatch.user.entity.CompanyUserRole;
import com.fleetmatch.user.entity.User;
import com.fleetmatch.user.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    List<User> findByStatus(UserStatus status);

    long countByStatus(UserStatus status);

    long countByCompanyId(UUID companyId);

    long countByCompanyIdAndStatus(UUID companyId, UserStatus status);

    long countByCompanyIdAndCompanyUserRoleAndStatus(
            UUID companyId,
            CompanyUserRole companyUserRole,
            UserStatus status
    );

    List<User> findByCompanyId(UUID companyId);
}
