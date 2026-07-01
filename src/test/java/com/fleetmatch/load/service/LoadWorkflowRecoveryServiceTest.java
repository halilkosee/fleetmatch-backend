package com.fleetmatch.load.service;

import com.fleetmatch.audit.service.AuditLogService;
import com.fleetmatch.company.entity.Company;
import com.fleetmatch.company.entity.CompanyType;
import com.fleetmatch.load.entity.Load;
import com.fleetmatch.load.entity.LoadStatus;
import com.fleetmatch.load.repository.LoadRepository;
import com.fleetmatch.messaging.service.MessagingService;
import com.fleetmatch.notification.inapp.service.NotificationService;
import com.fleetmatch.offer.entity.Offer;
import com.fleetmatch.offer.entity.OfferStatus;
import com.fleetmatch.offer.repository.OfferRepository;
import com.fleetmatch.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoadWorkflowRecoveryServiceTest {

    @Mock
    private LoadRepository loadRepository;
    @Mock
    private OfferRepository offerRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private MessagingService messagingService;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private LoadWorkflowRecoveryService recoveryService;

    @Test
    void expiredPostedLoadsMoveToExpired() {
        LocalDateTime now = LocalDateTime.now();
        Load load = load(LoadStatus.POSTED);
        load.setOfferDeadlineAt(now.minusMinutes(1));
        when(loadRepository.findTop50ByStatusAndOfferDeadlineAtBefore(LoadStatus.POSTED, now))
                .thenReturn(List.of(load));

        recoveryService.expirePostedLoads(now);

        assertEquals(LoadStatus.EXPIRED, load.getStatus());
        assertEquals(now, load.getExpiredAt());
        verify(loadRepository).save(load);
        verify(notificationService).createForCompany(
                load.getBrokerCompany(),
                com.fleetmatch.notification.event.NotificationType.LOAD_EXPIRED,
                "Load expired",
                "Your load expired without being booked",
                "LOAD",
                load.getId()
        );
    }

    @Test
    void staleFleetConfirmationRejectsOfferAndRepostsLoad() {
        ReflectionTestUtils.setField(recoveryService, "loadOfferExpirationMs", 60_000L);
        LocalDateTime now = LocalDateTime.now();
        Load load = load(LoadStatus.AWAITING_FLEET_CONFIRMATION);
        load.setConfirmationDeadlineAt(now.minusMinutes(1));
        Offer selected = offer(load, OfferStatus.SELECTED);
        when(loadRepository.findTop50ByStatusAndConfirmationDeadlineAtBefore(
                LoadStatus.AWAITING_FLEET_CONFIRMATION,
                now
        )).thenReturn(List.of(load));
        when(offerRepository.findFirstByLoadIdAndStatus(load.getId(), OfferStatus.SELECTED))
                .thenReturn(Optional.of(selected));

        recoveryService.releaseStaleFleetConfirmations(now);

        assertEquals(OfferStatus.REJECTED, selected.getStatus());
        assertEquals(LoadStatus.POSTED, load.getStatus());
        assertEquals(null, load.getConfirmationDeadlineAt());
        assertNotNull(load.getOfferDeadlineAt());
        assertTrue(load.getOfferDeadlineAt().isAfter(now));
        verify(offerRepository).save(selected);
        verify(loadRepository).save(load);
        verify(messagingService).archiveConversation(load.getId());
    }

    private Load load(LoadStatus status) {
        Company broker = new Company();
        broker.setId(UUID.randomUUID());
        broker.setLegalName("Broker");
        broker.setEmail("broker@example.test");
        broker.setType(CompanyType.BROKER);

        Load load = new Load();
        load.setId(UUID.randomUUID());
        load.setBrokerCompany(broker);
        load.setStatus(status);
        return load;
    }

    private Offer offer(Load load, OfferStatus status) {
        Company fleet = new Company();
        fleet.setId(UUID.randomUUID());
        fleet.setLegalName("Fleet");
        fleet.setEmail("fleet@example.test");
        fleet.setType(CompanyType.FLEET);

        User fleetUser = new User();
        fleetUser.setId(UUID.randomUUID());
        fleetUser.setCompany(fleet);

        Offer offer = new Offer();
        offer.setId(UUID.randomUUID());
        offer.setLoad(load);
        offer.setFleetUser(fleetUser);
        offer.setStatus(status);
        return offer;
    }
}
