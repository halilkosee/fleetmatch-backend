package com.fleetmatch.fleet.controller;

import com.fleetmatch.fleet.service.FleetService;
import com.fleetmatch.load.dto.LoadResponse;
import com.fleetmatch.offer.dto.OfferResponse;
import com.fleetmatch.security.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;


@RestController
@RequestMapping("/api/fleet")
@RequiredArgsConstructor
public class FleetController {

    private final FleetService fleetService;

    @GetMapping("/offers")
    public Page<OfferResponse> getMyOffers(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            Pageable pageable
    ) {
        return fleetService.getMyOffers(
                currentUser,
                pageable
        );
    }

    @GetMapping("/loads")
    public List<LoadResponse> getMyAcceptedLoads(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return fleetService.getMyAcceptedLoads(currentUser);
    }
}