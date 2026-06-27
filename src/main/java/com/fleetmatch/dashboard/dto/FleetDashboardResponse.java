package com.fleetmatch.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FleetDashboardResponse {
    private long submittedOffers;
    private long pendingOffers;
    private long acceptedOffers;
    private long rejectedOffers;
    private long bookedLoads;
    private long inTransitLoads;
    private long deliveredLoads;
    private long cancelledLoads;
}
