package com.fleetmatch.address.controller;

import com.fleetmatch.address.dto.AddressSuggestionResponse;
import com.fleetmatch.address.service.AddressLookupService;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/address")
@RequiredArgsConstructor
public class AddressLookupController {

    private final AddressLookupService addressLookupService;

    @GetMapping("/suggestions")
    public List<AddressSuggestionResponse> suggestions(
            @RequestParam("q") @Size(max = 255) String query
    ) {
        return addressLookupService.suggest(query);
    }
}
