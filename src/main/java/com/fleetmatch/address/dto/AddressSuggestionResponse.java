package com.fleetmatch.address.dto;

public record AddressSuggestionResponse(
        String label,
        String matchedAddress,
        String street,
        String city,
        String state,
        String zip,
        Double latitude,
        Double longitude
) {
}
