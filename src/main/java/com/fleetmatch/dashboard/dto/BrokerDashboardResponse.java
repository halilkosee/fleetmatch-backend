package com.fleetmatch.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BrokerDashboardResponse {
    private long activeLoads;
    private long postedLoads;
    private long bookedLoads;
    private long inTransitLoads;
    private long deliveredLoads;
    private long cancelledLoads;
    private long totalOffersReceived;
    private long pendingOffers;
    private long acceptedOffers;
}
