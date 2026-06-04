package com.fleetmatch.admin.controller;

import com.fleetmatch.admin.dto.AdminDashboardResponse;
import com.fleetmatch.admin.dto.PendingUserResponse;
import com.fleetmatch.admin.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @PutMapping("/users/{userId}/approve")
    public String approveUser(@PathVariable UUID userId) {
        adminService.approveUser(userId);
        return "User approved";
    }

    @PutMapping("/users/{userId}/suspend")
    public String suspendUser(@PathVariable UUID userId) {
        adminService.suspendUser(userId);
        return "User suspended";
    }

    @GetMapping("/users/pending")
    public List<PendingUserResponse> getPendingUsers() {
        return adminService.getPendingUsers();
    }

    @GetMapping("/dashboard")
    public AdminDashboardResponse getDashboard() {
        return adminService.getDashboard();
    }
}