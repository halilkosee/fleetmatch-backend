package com.fleetmatch.address.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetmatch.address.dto.AddressSuggestionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AddressLookupService {

    private static final String CENSUS_GEOCODER_URL =
            "https://geocoding.geo.census.gov/geocoder/locations/onelineaddress";
    private static final int MIN_QUERY_LENGTH = 6;
    private static final int MAX_RESULTS = 5;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    public Optional<AddressSuggestionResponse> bestMatch(String query) {
        return suggest(query).stream().findFirst();
    }

    public List<AddressSuggestionResponse> suggest(String query) {
        String normalizedQuery = query == null ? "" : query.trim();
        if (normalizedQuery.length() < MIN_QUERY_LENGTH) {
            return List.of();
        }

        try {
            URI uri = UriComponentsBuilder
                    .fromUriString(CENSUS_GEOCODER_URL)
                    .queryParam("address", normalizedQuery)
                    .queryParam("benchmark", "Public_AR_Current")
                    .queryParam("format", "json")
                    .build()
                    .toUri();

            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(5))
                    .header("Accept", "application/json")
                    .header("User-Agent", "EasyFleetMatch address lookup")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return List.of();
            }

            return parseSuggestions(response.body());
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private List<AddressSuggestionResponse> parseSuggestions(String responseBody) throws Exception {
        JsonNode matches = objectMapper
                .readTree(responseBody)
                .path("result")
                .path("addressMatches");

        if (!matches.isArray()) {
            return List.of();
        }

        List<AddressSuggestionResponse> suggestions = new ArrayList<>();
        for (JsonNode match : matches) {
            if (suggestions.size() >= MAX_RESULTS) {
                break;
            }

            JsonNode components = match.path("addressComponents");
            JsonNode coordinates = match.path("coordinates");
            String matchedAddress = text(match, "matchedAddress");

            suggestions.add(new AddressSuggestionResponse(
                    matchedAddress,
                    matchedAddress,
                    streetLine(components),
                    text(components, "city"),
                    text(components, "state"),
                    text(components, "zip"),
                    decimal(coordinates, "y"),
                    decimal(coordinates, "x")
            ));
        }

        return suggestions;
    }

    private String streetLine(JsonNode components) {
        String number = text(components, "fromAddress");
        String preDirection = text(components, "preDirection");
        String streetName = text(components, "streetName");
        String suffixType = text(components, "suffixType");
        String suffixDirection = text(components, "suffixDirection");

        return String.join(
                        " ",
                        List.of(number, preDirection, streetName, suffixType, suffixDirection)
                )
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? "" : value.asText("");
    }

    private Double decimal(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isNumber() ? value.asDouble() : null;
    }
}
