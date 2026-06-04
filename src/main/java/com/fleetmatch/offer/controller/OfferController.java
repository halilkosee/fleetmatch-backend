package com.fleetmatch.offer.controller;

import com.fleetmatch.offer.dto.CreateOfferRequest;
import com.fleetmatch.offer.dto.OfferResponse;
import com.fleetmatch.offer.service.OfferService;
import com.fleetmatch.security.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/loads/{loadId}/offers")
@RequiredArgsConstructor
public class OfferController {

    private final OfferService offerService;

    @PostMapping
    public OfferResponse createOffer(
            @PathVariable UUID loadId,
            @RequestBody CreateOfferRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return offerService.createOffer(loadId, request, currentUser);
    }

    @GetMapping
    public List<OfferResponse> getOffersForLoad(
            @PathVariable UUID loadId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return offerService.getOffersForLoad(loadId, currentUser);
    }

    @PutMapping("/{offerId}/accept")
    public OfferResponse acceptOffer(
            @PathVariable UUID loadId,
            @PathVariable UUID offerId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return offerService.acceptOffer(loadId, offerId, currentUser);
    }
}