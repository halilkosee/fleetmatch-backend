package com.fleetmatch.address.service;

import com.fleetmatch.company.entity.Company;
import com.fleetmatch.company.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HeadquartersAddressNormalizationJob {

    private static final String PENDING = "PENDING";
    private static final String VERIFIED = "VERIFIED";
    private static final String UNMATCHED = "UNMATCHED";

    private final CompanyRepository companyRepository;
    private final AddressLookupService addressLookupService;

    @Scheduled(
            initialDelayString = "${fleetmatch.address.normalization.initial-delay-ms:120000}",
            fixedDelayString = "${fleetmatch.address.normalization.fixed-delay-ms:3600000}"
    )
    @Transactional
    public void normalizePendingHeadquartersAddresses() {
        companyRepository
                .findTop25ByHeadquartersIsNotNullAndHeadquartersAddressVerificationStatus(PENDING)
                .forEach(this::normalize);
    }

    private void normalize(Company company) {
        addressLookupService.bestMatch(company.getHeadquarters())
                .ifPresentOrElse(match -> {
                    company.setNormalizedHeadquarters(match.matchedAddress());
                    company.setHeadquartersLatitude(match.latitude());
                    company.setHeadquartersLongitude(match.longitude());
                    company.setHeadquartersAddressVerified(true);
                    company.setHeadquartersAddressVerificationStatus(VERIFIED);
                }, () -> {
                    company.setHeadquartersAddressVerified(false);
                    company.setHeadquartersAddressVerificationStatus(UNMATCHED);
                });
    }
}
