package com.fleetmatch.auth.service;

import com.fleetmatch.auth.dto.AuthResponse;
import com.fleetmatch.auth.dto.RegisterRequest;
import com.fleetmatch.common.exception.AccountNotActiveException;
import com.fleetmatch.common.exception.ResourceAlreadyExistsException;
import com.fleetmatch.company.entity.Company;
import com.fleetmatch.company.repository.CompanyRepository;
import com.fleetmatch.user.entity.CompanyUserRole;
import com.fleetmatch.user.entity.PlatformRole;
import com.fleetmatch.user.entity.User;
import com.fleetmatch.user.entity.UserStatus;
import com.fleetmatch.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.fleetmatch.security.jwt.JwtService;
import com.fleetmatch.security.user.CustomUserDetails;
import com.fleetmatch.security.user.CustomUserDetailsService;
import org.springframework.security.authentication.BadCredentialsException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public void register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceAlreadyExistsException(
                    "Email already exists"
            );
        }

        Company company = new Company();
        company.setLegalName(request.getCompanyLegalName());
        company.setDbaName(request.getCompanyDbaName());
        company.setEmail(request.getCompanyEmail());
        company.setPhone(request.getCompanyPhone());
        company.setType(request.getCompanyType());

        company = companyRepository.save(company);

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPlatformRole(PlatformRole.USER);
        user.setCompanyUserRole(
                request.getCompanyUserRole() != null
                        ? request.getCompanyUserRole()
                        : CompanyUserRole.OWNER
        );
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        user.setCompany(company);

        userRepository.save(user);
    }

    public AuthResponse login(String email, String password) {

        CustomUserDetails userDetails =
                (CustomUserDetails) userDetailsService.loadUserByUsername(email);

        if (!passwordEncoder.matches(
                password,
                userDetails.getPassword()
        )) {
            throw new BadCredentialsException("Invalid credentials");
        }

        if (userDetails.getUser().getStatus() != UserStatus.ACTIVE) {
            throw new AccountNotActiveException(
                    "Account is pending verification"
            );
        }

        String token =
                jwtService.generateToken(userDetails.getUser());

        return new AuthResponse(token);
    }
}