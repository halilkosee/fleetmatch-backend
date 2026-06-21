package com.fleetmatch.broker.controller;

import com.fleetmatch.broker.service.BrokerService;
import com.fleetmatch.company.dto.CompanyUserResponse;
import com.fleetmatch.company.dto.CreateCompanyUserRequest;
import com.fleetmatch.company.dto.UpdateCompanyUserRoleRequest;
import com.fleetmatch.load.dto.LoadResponse;
import com.fleetmatch.security.user.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/api/broker")
@RequiredArgsConstructor
public class BrokerController {

    private final BrokerService brokerService;

    @GetMapping("/loads")
    public Page<LoadResponse> getMyLoads(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            Pageable pageable
    ) {
        return brokerService.getMyLoads(currentUser, pageable);
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public void createBrokerUser(
            @Valid @RequestBody CreateCompanyUserRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        brokerService.createBrokerUser(
                request,
                currentUser
        );
    }

    @GetMapping("/users")
    public List<CompanyUserResponse> getBrokerUsers(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return brokerService.getBrokerUsers(
                currentUser
        );
    }

    @DeleteMapping("/users/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBrokerUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        brokerService.deleteBrokerUser(
                userId,
                currentUser
        );
    }

    @PutMapping("/users/{userId}/role")
    public void updateBrokerUserRole(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateCompanyUserRoleRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        brokerService.updateBrokerUserRole(
                userId,
                request,
                currentUser
        );
    }
}
