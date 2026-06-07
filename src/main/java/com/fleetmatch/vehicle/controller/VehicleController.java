package com.fleetmatch.vehicle.controller;

import com.fleetmatch.security.user.CustomUserDetails;
import com.fleetmatch.vehicle.dto.CreateVehicleRequest;
import com.fleetmatch.vehicle.dto.UpdateVehicleRequest;
import com.fleetmatch.vehicle.dto.VehicleResponse;
import com.fleetmatch.vehicle.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;

    @PostMapping
    public VehicleResponse createVehicle(
            @RequestBody CreateVehicleRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return vehicleService.createVehicle(
                request,
                currentUser
        );
    }

    @GetMapping("/my")
    public List<VehicleResponse> getMyVehicles(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return vehicleService.getMyVehicles(
                currentUser
        );
    }

    @GetMapping("/{vehicleId}")
    public VehicleResponse getVehicle(
            @PathVariable UUID vehicleId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return vehicleService.getVehicle(
                vehicleId,
                currentUser
        );
    }

    @PutMapping("/{vehicleId}")
    public VehicleResponse updateVehicle(
            @PathVariable UUID vehicleId,
            @RequestBody UpdateVehicleRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return vehicleService.updateVehicle(
                vehicleId,
                request,
                currentUser
        );
    }

    @DeleteMapping("/{vehicleId}")
    public void deleteVehicle(
            @PathVariable UUID vehicleId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        vehicleService.deleteVehicle(
                vehicleId,
                currentUser
        );
    }
}