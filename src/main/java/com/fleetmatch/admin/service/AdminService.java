package com.fleetmatch.admin.service;

import com.fleetmatch.admin.dto.PendingUserResponse;
import java.util.List;
import com.fleetmatch.common.exception.ResourceNotFoundException;
import com.fleetmatch.user.entity.User;
import com.fleetmatch.user.entity.UserStatus;
import com.fleetmatch.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.fleetmatch.admin.dto.AdminDashboardResponse;
import com.fleetmatch.company.entity.CompanyType;
import com.fleetmatch.company.repository.CompanyRepository;
import com.fleetmatch.load.entity.LoadStatus;
import com.fleetmatch.load.repository.LoadRepository;
import com.fleetmatch.offer.entity.OfferStatus;
import com.fleetmatch.offer.repository.OfferRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;

    private final CompanyRepository companyRepository;
    private final LoadRepository loadRepository;
    private final OfferRepository offerRepository;

    public void approveUser(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        user.setStatus(UserStatus.ACTIVE);

        userRepository.save(user);
    }

    public void suspendUser(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        user.setStatus(UserStatus.SUSPENDED);

        userRepository.save(user);


    }

    public List<PendingUserResponse> getPendingUsers() {

        return userRepository.findByStatus(UserStatus.PENDING_VERIFICATION)
                .stream()
                .map(user -> new PendingUserResponse(
                        user.getId(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getEmail(),
                        user.getPlatformRole(),
                        user.getStatus(),
                        user.getCompany() != null
                                ? user.getCompany().getLegalName()
                                : null
                ))
                .toList();
    }

    public AdminDashboardResponse getDashboard() {

        var users = new AdminDashboardResponse.UserStats(
                userRepository.countByStatus(UserStatus.PENDING_VERIFICATION),
                userRepository.countByStatus(UserStatus.ACTIVE),
                userRepository.countByStatus(UserStatus.SUSPENDED)
        );

        var companies = new AdminDashboardResponse.CompanyStats(
                companyRepository.countByType(CompanyType.BROKER),
                companyRepository.countByType(CompanyType.FLEET)
        );

        var loads = new AdminDashboardResponse.LoadStats(
                loadRepository.countByStatus(LoadStatus.POSTED),
                loadRepository.countByStatus(LoadStatus.AWAITING_FLEET_CONFIRMATION),
                loadRepository.countByStatus(LoadStatus.BOOKED),
                loadRepository.countByStatus(LoadStatus.IN_TRANSIT),
                loadRepository.countByStatus(LoadStatus.DELIVERED),
                loadRepository.countByStatus(LoadStatus.CANCELLED)
        );

        var offers = new AdminDashboardResponse.OfferStats(
                offerRepository.countByStatus(OfferStatus.PENDING),
                offerRepository.countByStatus(OfferStatus.SELECTED),
                offerRepository.countByStatus(OfferStatus.CONFIRMED),
                offerRepository.countByStatus(OfferStatus.REJECTED),
                offerRepository.countByStatus(OfferStatus.WITHDRAWN)
        );

        return new AdminDashboardResponse(
                users,
                companies,
                loads,
                offers
        );
    }
}
