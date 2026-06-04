package com.fleetmatch.broker.controller;

import com.fleetmatch.broker.service.BrokerService;
import com.fleetmatch.load.dto.LoadResponse;
import com.fleetmatch.security.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


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
}