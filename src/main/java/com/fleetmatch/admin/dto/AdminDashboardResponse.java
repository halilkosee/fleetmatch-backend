package com.fleetmatch.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminDashboardResponse {

    private UserStats users;
    private CompanyStats companies;
    private LoadStats loads;
    private OfferStats offers;

    @Getter
    @AllArgsConstructor
    public static class UserStats {
        private long pending;
        private long active;
        private long suspended;
    }

    @Getter
    @AllArgsConstructor
    public static class CompanyStats {
        private long brokers;
        private long fleets;
    }

    @Getter
    @AllArgsConstructor
    public static class LoadStats {
        private long posted;
        private long awaitingFleetConfirmation;
        private long booked;
        private long inTransit;
        private long delivered;
        private long cancelled;
    }

    @Getter
    @AllArgsConstructor
    public static class OfferStats {
        private long pending;
        private long selected;
        private long confirmed;
        private long rejected;
        private long withdrawn;
    }
}
