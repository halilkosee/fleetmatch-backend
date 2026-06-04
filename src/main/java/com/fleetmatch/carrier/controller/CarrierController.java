package com.fleetmatch.carrier.controller;

import com.fleetmatch.carrier.service.CarrierService;
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
@RequestMapping("/api/carrier")
@RequiredArgsConstructor
public class CarrierController {

    private final CarrierService carrierService;

    @GetMapping("/offers")
    public Page<OfferResponse> getMyOffers(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            Pageable pageable
    ) {
        return carrierService.getMyOffers(
                currentUser,
                pageable
        );
    }

    @GetMapping("/loads")
    public List<LoadResponse> getMyAcceptedLoads(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return carrierService.getMyAcceptedLoads(currentUser);
    }
}