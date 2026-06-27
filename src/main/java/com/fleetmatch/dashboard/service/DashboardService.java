package com.fleetmatch.dashboard.service;

import com.fleetmatch.common.exception.ResourceNotFoundException;
import com.fleetmatch.company.entity.CompanyType;
import com.fleetmatch.dashboard.dto.BrokerDashboardResponse;
import com.fleetmatch.dashboard.dto.FleetDashboardResponse;
import com.fleetmatch.load.entity.LoadStatus;
import com.fleetmatch.load.repository.LoadRepository;
import com.fleetmatch.offer.entity.OfferStatus;
import com.fleetmatch.offer.repository.OfferRepository;
import com.fleetmatch.security.user.CustomUserDetails;
import com.fleetmatch.user.entity.User;
import com.fleetmatch.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final LoadRepository loadRepository;
    private final OfferRepository offerRepository;

    public BrokerDashboardResponse brokerDashboard(CustomUserDetails currentUser) {
        User user = getCurrentUser(currentUser);
        if (user.getCompany() == null || user.getCompany().getType() != CompanyType.BROKER) {
            throw new AccessDeniedException("Only broker companies can access broker dashboard");
        }

        var companyId = user.getCompany().getId();
        long posted = loadRepository.countByBrokerCompanyIdAndStatus(companyId, LoadStatus.POSTED);
        long booked = loadRepository.countByBrokerCompanyIdAndStatus(companyId, LoadStatus.BOOKED);
        long inTransit = loadRepository.countByBrokerCompanyIdAndStatus(companyId, LoadStatus.IN_TRANSIT);
        long delivered = loadRepository.countByBrokerCompanyIdAndStatus(companyId, LoadStatus.DELIVERED);
        long cancelled = loadRepository.countByBrokerCompanyIdAndStatus(companyId, LoadStatus.CANCELLED);

        return new BrokerDashboardResponse(
                posted + booked + inTransit,
                posted,
                booked,
                inTransit,
                delivered,
                cancelled,
                offerRepository.countByBrokerCompanyId(companyId),
                offerRepository.countByBrokerCompanyIdAndStatus(companyId, OfferStatus.PENDING),
                offerRepository.countByBrokerCompanyIdAndStatus(companyId, OfferStatus.CONFIRMED)
        );
    }

    public FleetDashboardResponse fleetDashboard(CustomUserDetails currentUser) {
        User user = getCurrentUser(currentUser);
        if (user.getCompany() == null || user.getCompany().getType() != CompanyType.FLEET) {
            throw new AccessDeniedException("Only fleet companies can access fleet dashboard");
        }

        var companyId = user.getCompany().getId();

        return new FleetDashboardResponse(
                offerRepository.countByFleetUserCompanyId(companyId),
                offerRepository.countByFleetUserCompanyIdAndStatus(companyId, OfferStatus.PENDING),
                offerRepository.countByFleetUserCompanyIdAndStatus(companyId, OfferStatus.CONFIRMED),
                offerRepository.countByFleetUserCompanyIdAndStatus(companyId, OfferStatus.REJECTED),
                offerRepository.countConfirmedFleetLoadsByStatus(companyId, LoadStatus.BOOKED),
                offerRepository.countConfirmedFleetLoadsByStatus(companyId, LoadStatus.IN_TRANSIT),
                offerRepository.countConfirmedFleetLoadsByStatus(companyId, LoadStatus.DELIVERED),
                offerRepository.countConfirmedFleetLoadsByStatus(companyId, LoadStatus.CANCELLED)
        );
    }

    private User getCurrentUser(CustomUserDetails currentUser) {
        return userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
