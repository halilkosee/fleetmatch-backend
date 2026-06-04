package com.fleetmatch.load.controller;

import com.fleetmatch.load.dto.CreateLoadRequest;
import com.fleetmatch.load.dto.LoadResponse;
import com.fleetmatch.load.entity.EquipmentType;
import com.fleetmatch.load.service.LoadService;
import com.fleetmatch.security.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/loads")
@RequiredArgsConstructor
public class LoadController {

    private final LoadService loadService;

    @PostMapping
    public LoadResponse createLoad(
            @RequestBody CreateLoadRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return loadService.createLoad(request, currentUser);
    }

    @GetMapping
    public Page<LoadResponse> getPostedLoads(Pageable pageable) {
        return loadService.getPostedLoads(pageable);
    }

    @GetMapping("/search")
    public List<LoadResponse> searchLoads(
            @RequestParam(required = false) String pickupState,
            @RequestParam(required = false) String deliveryState,
            @RequestParam(required = false) EquipmentType equipmentType
    ) {
        return loadService.searchLoads(
                pickupState,
                deliveryState,
                equipmentType
        );
    }

    @GetMapping("/{loadId}")
    public LoadResponse getLoadById(
            @PathVariable UUID loadId
    ) {
        return loadService.getLoadById(loadId);
    }

    @PutMapping("/{loadId}/start")
    public LoadResponse startLoad(
            @PathVariable UUID loadId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return loadService.startLoad(loadId, currentUser);
    }

    @PutMapping("/{loadId}/deliver")
    public LoadResponse deliverLoad(
            @PathVariable UUID loadId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return loadService.deliverLoad(loadId, currentUser);
    }

    @PutMapping("/{loadId}/cancel")
    public LoadResponse cancelLoad(
            @PathVariable UUID loadId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return loadService.cancelLoad(loadId, currentUser);
    }
}