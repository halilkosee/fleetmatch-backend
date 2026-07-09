package com.fleetmatch.vehicle.service;

import com.fleetmatch.audit.service.AuditLogService;
import com.fleetmatch.company.entity.Company;
import com.fleetmatch.company.entity.CompanyType;
import com.fleetmatch.company.entity.CompanyVerificationStatus;
import com.fleetmatch.security.user.CustomUserDetails;
import com.fleetmatch.subscription.service.SubscriptionValidationService;
import com.fleetmatch.user.entity.User;
import com.fleetmatch.user.repository.UserRepository;
import com.fleetmatch.vehicle.dto.CreateVehicleRequest;
import com.fleetmatch.vehicle.entity.Vehicle;
import com.fleetmatch.vehicle.entity.VehicleType;
import com.fleetmatch.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SubscriptionValidationService subscriptionValidationService;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private VehicleService vehicleService;

    @Test
    void onboardingFleetCanCreateVehicleBeforeSubscriptionExists() {
        User user = user(CompanyVerificationStatus.PENDING);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> {
            Vehicle vehicle = invocation.getArgument(0);
            vehicle.setId(UUID.randomUUID());
            return vehicle;
        });

        var response = vehicleService.createVehicle(request(), new CustomUserDetails(user));

        assertEquals("TX-1000", response.getPlateNumber());
        verify(subscriptionValidationService, never()).validateVehicleLimit(any());
    }

    @Test
    void approvedFleetVehicleCreationStillChecksSubscriptionLimit() {
        User user = user(CompanyVerificationStatus.APPROVED);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> {
            Vehicle vehicle = invocation.getArgument(0);
            vehicle.setId(UUID.randomUUID());
            return vehicle;
        });

        vehicleService.createVehicle(request(), new CustomUserDetails(user));

        verify(subscriptionValidationService).validateVehicleLimit(user.getCompany());
    }

    private CreateVehicleRequest request() {
        CreateVehicleRequest request = new CreateVehicleRequest();
        request.setPlateNumber("TX-1000");
        request.setType(VehicleType.BOX_TRUCK);
        request.setLengthFeet(26);
        return request;
    }

    private User user(CompanyVerificationStatus verificationStatus) {
        Company company = new Company();
        company.setId(UUID.randomUUID());
        company.setLegalName("Fleet LLC");
        company.setEmail("ops@fleet.test");
        company.setType(CompanyType.FLEET);
        company.setVerificationStatus(verificationStatus);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("owner@fleet.test");
        user.setPassword("encoded");
        user.setCompany(company);
        return user;
    }
}
