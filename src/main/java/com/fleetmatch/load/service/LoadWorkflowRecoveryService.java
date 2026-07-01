package com.fleetmatch.load.service;

import com.fleetmatch.audit.entity.AuditAction;
import com.fleetmatch.audit.service.AuditLogService;
import com.fleetmatch.load.entity.Load;
import com.fleetmatch.load.entity.LoadStatus;
import com.fleetmatch.load.repository.LoadRepository;
import com.fleetmatch.messaging.service.MessagingService;
import com.fleetmatch.notification.event.NotificationType;
import com.fleetmatch.notification.inapp.service.NotificationService;
import com.fleetmatch.offer.entity.Offer;
import com.fleetmatch.offer.entity.OfferStatus;
import com.fleetmatch.offer.repository.OfferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LoadWorkflowRecoveryService {

    private final LoadRepository loadRepository;
    private final OfferRepository offerRepository;
    private final NotificationService notificationService;
    private final MessagingService messagingService;
    private final AuditLogService auditLogService;

    @Value("${fleetmatch.workflow.load-offer-expiration-ms:172800000}")
    private long loadOfferExpirationMs = 172800000L;

    @Scheduled(
            initialDelayString = "${fleetmatch.workflow.recovery.initial-delay-ms:60000}",
            fixedDelayString = "${fleetmatch.workflow.recovery.fixed-delay-ms:300000}"
    )
    @Transactional
    public void recoverTimedOutWorkflows() {
        LocalDateTime now = LocalDateTime.now();
        expirePostedLoads(now);
        releaseStaleFleetConfirmations(now);
    }

    void expirePostedLoads(LocalDateTime now) {
        loadRepository
                .findTop50ByStatusAndOfferDeadlineAtBefore(LoadStatus.POSTED, now)
                .forEach(load -> expireLoad(load, now));
    }

    void releaseStaleFleetConfirmations(LocalDateTime now) {
        loadRepository
                .findTop50ByStatusAndConfirmationDeadlineAtBefore(
                        LoadStatus.AWAITING_FLEET_CONFIRMATION,
                        now
                )
                .forEach(load -> releaseFleetConfirmation(load, now));
    }

    private void expireLoad(Load load, LocalDateTime now) {
        load.setStatus(LoadStatus.EXPIRED);
        load.setExpiredAt(now);
        load.setConfirmationDeadlineAt(null);
        loadRepository.save(load);

        notificationService.createForCompany(
                load.getBrokerCompany(),
                NotificationType.LOAD_EXPIRED,
                "Load expired",
                "Your load expired without being booked",
                "LOAD",
                load.getId()
        );
        auditLogService.log(
                null,
                AuditAction.LOAD_EXPIRED,
                "LOAD",
                load.getId(),
                "Load expired automatically after offer deadline"
        );
    }

    private void releaseFleetConfirmation(Load load, LocalDateTime now) {
        offerRepository.findFirstByLoadIdAndStatus(load.getId(), OfferStatus.SELECTED)
                .ifPresent(offer -> rejectTimedOutOffer(load, offer));

        load.setStatus(LoadStatus.POSTED);
        load.setConfirmationDeadlineAt(null);
        load.setOfferDeadlineAt(now.plus(Duration.ofMillis(loadOfferExpirationMs)));
        loadRepository.save(load);
        messagingService.archiveConversation(load.getId());

        notificationService.createForCompany(
                load.getBrokerCompany(),
                NotificationType.FLEET_CONFIRMATION_TIMEOUT,
                "Fleet confirmation timed out",
                "The selected fleet did not confirm in time. Your load is posted again.",
                "LOAD",
                load.getId()
        );
        auditLogService.log(
                null,
                AuditAction.FLEET_CONFIRMATION_TIMEOUT,
                "LOAD",
                load.getId(),
                "Fleet confirmation timed out and load was reposted"
        );
    }

    private void rejectTimedOutOffer(Load load, Offer offer) {
        offer.setStatus(OfferStatus.REJECTED);
        offerRepository.save(offer);
        notificationService.createForCompany(
                offer.getFleetUser().getCompany(),
                NotificationType.FLEET_CONFIRMATION_TIMEOUT,
                "Confirmation timed out",
                "Your selected offer was released because it was not confirmed in time.",
                "OFFER",
                offer.getId()
        );
        auditLogService.log(
                null,
                AuditAction.OFFER_REJECTED,
                "OFFER",
                offer.getId(),
                "Offer rejected automatically after fleet confirmation timeout for load " + load.getId()
        );
    }
}
