package com.fleetmatch.dashboard.controller;

import com.fleetmatch.dashboard.dto.BrokerDashboardResponse;
import com.fleetmatch.dashboard.dto.FleetDashboardResponse;
import com.fleetmatch.dashboard.service.DashboardService;
import com.fleetmatch.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/broker")
    @Operation(summary = "Get broker dashboard KPIs")
    public BrokerDashboardResponse brokerDashboard(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return dashboardService.brokerDashboard(currentUser);
    }

    @GetMapping("/fleet")
    @Operation(summary = "Get fleet dashboard KPIs")
    public FleetDashboardResponse fleetDashboard(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return dashboardService.fleetDashboard(currentUser);
    }
}
