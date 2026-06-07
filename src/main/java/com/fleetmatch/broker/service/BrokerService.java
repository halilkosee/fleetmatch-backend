package com.fleetmatch.broker.service;

import com.fleetmatch.common.exception.ResourceNotFoundException;
import com.fleetmatch.company.entity.CompanyType;
import com.fleetmatch.load.dto.LoadResponse;
import com.fleetmatch.load.entity.Load;
import com.fleetmatch.load.repository.LoadRepository;
import com.fleetmatch.security.user.CustomUserDetails;
import com.fleetmatch.user.entity.User;
import com.fleetmatch.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BrokerService {

    private final UserRepository userRepository;
    private final LoadRepository loadRepository;

    public Page<LoadResponse> getMyLoads(
            CustomUserDetails currentUser,
            Pageable pageable
    ) {

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getCompany() == null ||
                user.getCompany().getType() != CompanyType.BROKER) {
            throw new AccessDeniedException("Only brokers can access this endpoint");
        }

        return loadRepository.findByBrokerCompanyId(
                user.getCompany().getId(),
                pageable
        ).map(this::toResponse);
    }

    private LoadResponse toResponse(Load load) {
        return new LoadResponse(
                load.getId(),

                load.getPickupCity(),
                load.getPickupState(),
                load.getPickupDate(),

                load.getDeliveryCity(),
                load.getDeliveryState(),
                load.getDeliveryDate(),

                load.getEquipmentType(),

                load.getWeight(),
                load.getRate(),
                load.getMiles(),

                load.getCommodity(),
                load.getReferenceNumber(),

                load.getStatus(),
                load.getNotes(),

                load.getBrokerCompany().getLegalName(),
                null,
                null
        );
    }
}