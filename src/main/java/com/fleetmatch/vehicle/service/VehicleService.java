package com.fleetmatch.vehicle.service;

import com.fleetmatch.audit.entity.AuditAction;
import com.fleetmatch.audit.service.AuditLogService;
import com.fleetmatch.common.exception.BusinessRuleException;
import com.fleetmatch.common.exception.ResourceNotFoundException;
import com.fleetmatch.company.entity.Company;
import com.fleetmatch.company.entity.CompanyType;
import com.fleetmatch.company.entity.CompanyVerificationStatus;
import com.fleetmatch.security.user.CustomUserDetails;
import com.fleetmatch.subscription.service.SubscriptionValidationService;
import com.fleetmatch.user.entity.User;
import com.fleetmatch.user.repository.UserRepository;
import com.fleetmatch.vehicle.dto.CreateVehicleRequest;
import com.fleetmatch.vehicle.dto.VehicleResponse;
import com.fleetmatch.vehicle.entity.Vehicle;
import com.fleetmatch.vehicle.repository.VehicleRepository;
import com.fleetmatch.vehicle.dto.UpdateVehicleRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final SubscriptionValidationService
            subscriptionValidationService;
    private final AuditLogService auditLogService;
    public VehicleResponse createVehicle(
            CreateVehicleRequest request,
            CustomUserDetails currentUser
    ) {

        User user = getCurrentUser(currentUser);

        Company company = requireFleetCompany(user);

        if (company.getVerificationStatus() == CompanyVerificationStatus.APPROVED) {
            subscriptionValidationService
                    .validateVehicleLimit(company);
        }

        if (vehicleRepository.existsByPlateNumber(
                request.getPlateNumber()
        )) {
            throw new BusinessRuleException(
                    "Plate number already exists"
            );
        }

        if (request.getVinNumber() != null &&
                vehicleRepository.existsByVinNumber(
                request.getVinNumber()
        )) {
            throw new BusinessRuleException(
                    "VIN number already exists"
            );
        }

        Vehicle vehicle = new Vehicle();

        vehicle.setCompany(company);
        vehicle.setPlateNumber(request.getPlateNumber());
        vehicle.setVinNumber(request.getVinNumber());
        vehicle.setType(request.getType());
        vehicle.setLengthFeet(request.getLengthFeet());
        vehicle.setCapabilities(request.getCapabilities());
        vehicle.setMake(request.getMake());
        vehicle.setModel(request.getModel());
        vehicle.setYear(request.getYear());

        Vehicle savedVehicle =
                vehicleRepository.save(vehicle);
        auditLogService.log(
                user,
                AuditAction.VEHICLE_CREATED,
                "VEHICLE",
                savedVehicle.getId(),
                "Vehicle created"
        );

        return mapToResponse(savedVehicle);
    }

    public List<VehicleResponse> getMyVehicles(
            CustomUserDetails currentUser
    ) {

        User user = getCurrentUser(currentUser);

        Company company = requireFleetCompany(user);

        return vehicleRepository.findByCompanyIdAndActiveTrue(
                        company.getId()
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private User getCurrentUser(
            CustomUserDetails currentUser
    ) {

        return userRepository.findById(
                        currentUser.getId()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        ));
    }

    private VehicleResponse mapToResponse(
            Vehicle vehicle
    ) {

        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getPlateNumber(),
                vehicle.getVinNumber(),
                vehicle.getType(),
                vehicle.getLengthFeet(),
                vehicle.getCapabilities(),
                vehicle.getMake(),
                vehicle.getModel(),
                vehicle.getYear(),
                vehicle.getActive()
        );
    }

    public VehicleResponse getVehicle(
            UUID vehicleId,
            CustomUserDetails currentUser
    ) {

        User user = getCurrentUser(currentUser);
        Company company = requireFleetCompany(user);

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Vehicle not found"
                        ));

        if (!vehicle.getCompany().getId()
                .equals(company.getId())) {

            throw new BusinessRuleException(
                    "You do not have access to this vehicle"
            );
        }

        if (!vehicle.getActive()) {
            throw new ResourceNotFoundException(
                    "Vehicle not found"
            );
        }

        return mapToResponse(vehicle);
    }

    public VehicleResponse updateVehicle(
            UUID vehicleId,
            UpdateVehicleRequest request,
            CustomUserDetails currentUser
    ) {

        User user = getCurrentUser(currentUser);
        Company company = requireFleetCompany(user);

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Vehicle not found"
                        ));

        if (!vehicle.getCompany().getId()
                .equals(company.getId())) {

            throw new BusinessRuleException(
                    "You do not have access to this vehicle"
            );
        }

        if (!vehicle.getActive()) {
            throw new ResourceNotFoundException(
                    "Vehicle not found"
            );
        }

        if (vehicleRepository.existsByPlateNumberAndIdNot(
                request.getPlateNumber(),
                vehicle.getId()
        )) {
            throw new BusinessRuleException(
                    "Plate number already exists"
            );
        }

        if (request.getVinNumber() != null &&
                vehicleRepository.existsByVinNumberAndIdNot(
                request.getVinNumber(),
                vehicle.getId()
        )) {
            throw new BusinessRuleException(
                    "VIN number already exists"
            );
        }

        vehicle.setPlateNumber(request.getPlateNumber());
        vehicle.setVinNumber(request.getVinNumber());
        vehicle.setType(request.getType());
        vehicle.setLengthFeet(request.getLengthFeet());
        vehicle.setCapabilities(request.getCapabilities());
        vehicle.setMake(request.getMake());
        vehicle.setModel(request.getModel());
        vehicle.setYear(request.getYear());

        Vehicle updatedVehicle =
                vehicleRepository.save(vehicle);
        auditLogService.log(
                user,
                AuditAction.VEHICLE_UPDATED,
                "VEHICLE",
                updatedVehicle.getId(),
                "Vehicle updated"
        );

        return mapToResponse(updatedVehicle);
    }

    public void deleteVehicle(
            UUID vehicleId,
            CustomUserDetails currentUser
    ) {

        User user = getCurrentUser(currentUser);
        Company company = requireFleetCompany(user);

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Vehicle not found"
                        ));

        if (!vehicle.getCompany().getId()
                .equals(company.getId())) {

            throw new BusinessRuleException(
                    "You do not have access to this vehicle"
            );
        }

        if (!vehicle.getActive()) {
            throw new ResourceNotFoundException(
                    "Vehicle not found"
            );
        }

        vehicle.setActive(false);

        Vehicle saved = vehicleRepository.save(vehicle);
        auditLogService.log(
                user,
                AuditAction.VEHICLE_DELETED,
                "VEHICLE",
                saved.getId(),
                "Vehicle soft deleted"
        );
    }

    private Company requireFleetCompany(User user) {
        Company company = user.getCompany();

        if (company == null || company.getType() != CompanyType.FLEET) {
            throw new BusinessRuleException(
                    "Only fleet users can access vehicles"
            );
        }

        return company;
    }

}
