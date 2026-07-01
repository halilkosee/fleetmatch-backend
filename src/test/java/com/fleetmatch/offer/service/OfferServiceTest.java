package com.fleetmatch.offer.service;

import com.fleetmatch.audit.service.AuditLogService;
import com.fleetmatch.common.exception.BusinessRuleException;
import com.fleetmatch.company.entity.Company;
import com.fleetmatch.company.entity.CompanyType;
import com.fleetmatch.company.entity.CompanyVerificationStatus;
import com.fleetmatch.load.entity.EquipmentType;
import com.fleetmatch.load.entity.Load;
import com.fleetmatch.load.entity.LoadStatus;
import com.fleetmatch.load.repository.LoadRepository;
import com.fleetmatch.messaging.entity.Conversation;
import com.fleetmatch.messaging.service.MessagingService;
import com.fleetmatch.notification.inapp.service.NotificationService;
import com.fleetmatch.offer.dto.CreateOfferRequest;
import com.fleetmatch.offer.dto.OfferResponse;
import com.fleetmatch.offer.entity.Offer;
import com.fleetmatch.offer.entity.OfferStatus;
import com.fleetmatch.offer.repository.OfferRepository;
import com.fleetmatch.security.user.CustomUserDetails;
import com.fleetmatch.subscription.service.SubscriptionValidationService;
import com.fleetmatch.user.entity.User;
import com.fleetmatch.user.entity.UserStatus;
import com.fleetmatch.user.repository.UserRepository;
import com.fleetmatch.user.service.UserVerificationGuard;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OfferServiceTest {

    @Mock
    private OfferRepository offerRepository;
    @Mock
    private LoadRepository loadRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SubscriptionValidationService subscriptionValidationService;
    @Mock
    private MessagingService messagingService;
    @Mock
    private UserVerificationGuard userVerificationGuard;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private OfferService offerService;

    @Test
    void approvedFleetCanCreateOfferForPostedLoad() {
        User broker = user(CompanyType.BROKER);
        User fleet = user(CompanyType.FLEET);
        Load load = load(broker, LoadStatus.POSTED);
        CreateOfferRequest request = new CreateOfferRequest();
        request.setAmount(BigDecimal.valueOf(2200));
        request.setMessage("Can cover.");
        when(userRepository.findById(fleet.getId())).thenReturn(Optional.of(fleet));
        when(loadRepository.findByIdForUpdate(load.getId())).thenReturn(Optional.of(load));
        when(offerRepository.existsByLoadIdAndFleetUserCompanyIdAndStatusIn(
                load.getId(),
                fleet.getCompany().getId(),
                List.of(OfferStatus.PENDING, OfferStatus.SELECTED, OfferStatus.CONFIRMED)
        )).thenReturn(false);
        when(offerRepository.save(any(Offer.class))).thenAnswer(invocation -> {
            Offer offer = invocation.getArgument(0);
            offer.setId(UUID.randomUUID());
            return offer;
        });

        OfferResponse response = offerService.createOffer(
                load.getId(),
                request,
                new CustomUserDetails(fleet)
        );

        assertEquals(OfferStatus.PENDING, response.getStatus());
        assertEquals(load.getId(), response.getLoadId());
        verify(subscriptionValidationService).validateCanSubmitOffer(fleet.getCompany());
    }

    @Test
    void companyCannotCreateSecondActiveOfferForSameLoad() {
        User broker = user(CompanyType.BROKER);
        User fleet = user(CompanyType.FLEET);
        Load load = load(broker, LoadStatus.POSTED);
        CreateOfferRequest request = new CreateOfferRequest();
        request.setAmount(BigDecimal.valueOf(2200));
        when(userRepository.findById(fleet.getId())).thenReturn(Optional.of(fleet));
        when(loadRepository.findByIdForUpdate(load.getId())).thenReturn(Optional.of(load));
        when(offerRepository.existsByLoadIdAndFleetUserCompanyIdAndStatusIn(
                load.getId(),
                fleet.getCompany().getId(),
                List.of(OfferStatus.PENDING, OfferStatus.SELECTED, OfferStatus.CONFIRMED)
        )).thenReturn(true);

        assertThrows(
                BusinessRuleException.class,
                () -> offerService.createOffer(
                        load.getId(),
                        request,
                        new CustomUserDetails(fleet)
                )
        );

        verify(offerRepository, never()).save(any());
    }

    @Test
    void expiredPostedLoadCannotReceiveOfferBeforeRecoveryJobRuns() {
        User broker = user(CompanyType.BROKER);
        User fleet = user(CompanyType.FLEET);
        Load load = load(broker, LoadStatus.POSTED);
        load.setOfferDeadlineAt(LocalDateTime.now().minusMinutes(1));
        CreateOfferRequest request = new CreateOfferRequest();
        request.setAmount(BigDecimal.valueOf(2200));
        when(userRepository.findById(fleet.getId())).thenReturn(Optional.of(fleet));
        when(loadRepository.findByIdForUpdate(load.getId())).thenReturn(Optional.of(load));

        assertThrows(
                BusinessRuleException.class,
                () -> offerService.createOffer(
                        load.getId(),
                        request,
                        new CustomUserDetails(fleet)
                )
        );

        assertEquals(LoadStatus.EXPIRED, load.getStatus());
        verify(loadRepository).save(load);
        verify(offerRepository, never()).save(any());
    }

    @Test
    void selectedOfferMovesLoadToAwaitingFleetConfirmation() {
        User broker = user(CompanyType.BROKER);
        User fleet = user(CompanyType.FLEET);
        Load load = load(broker, LoadStatus.POSTED);
        Offer offer = offer(load, fleet, OfferStatus.PENDING);
        Conversation conversation = new Conversation();
        conversation.setId(UUID.randomUUID());
        when(userRepository.findById(broker.getId())).thenReturn(Optional.of(broker));
        when(loadRepository.findByIdForUpdate(load.getId())).thenReturn(Optional.of(load));
        when(offerRepository.findByIdWithLoadForUpdate(offer.getId())).thenReturn(Optional.of(offer));
        when(loadRepository.save(load)).thenReturn(load);
        when(offerRepository.save(offer)).thenReturn(offer);
        when(messagingService.createConversationForSelectedOffer(offer)).thenReturn(conversation);

        OfferResponse response = offerService.selectOffer(
                load.getId(),
                offer.getId(),
                new CustomUserDetails(broker)
        );

        assertEquals(OfferStatus.SELECTED, response.getStatus());
        assertEquals(LoadStatus.AWAITING_FLEET_CONFIRMATION, load.getStatus());
        verify(messagingService).createConversationForSelectedOffer(offer);
    }

    @Test
    void expiredPostedLoadCannotBeSelectedBeforeRecoveryJobRuns() {
        User broker = user(CompanyType.BROKER);
        User fleet = user(CompanyType.FLEET);
        Load load = load(broker, LoadStatus.POSTED);
        load.setOfferDeadlineAt(LocalDateTime.now().minusMinutes(1));
        Offer offer = offer(load, fleet, OfferStatus.PENDING);
        when(userRepository.findById(broker.getId())).thenReturn(Optional.of(broker));
        when(loadRepository.findByIdForUpdate(load.getId())).thenReturn(Optional.of(load));
        when(offerRepository.findByIdWithLoadForUpdate(offer.getId())).thenReturn(Optional.of(offer));

        assertThrows(
                BusinessRuleException.class,
                () -> offerService.selectOffer(
                        load.getId(),
                        offer.getId(),
                        new CustomUserDetails(broker)
                )
        );

        assertEquals(LoadStatus.EXPIRED, load.getStatus());
        verify(loadRepository).save(load);
    }

    @Test
    void selectedFleetCanConfirmAssignmentAndRejectOtherPendingOffers() {
        User broker = user(CompanyType.BROKER);
        User fleet = user(CompanyType.FLEET);
        User otherFleet = user(CompanyType.FLEET);
        Load load = load(broker, LoadStatus.AWAITING_FLEET_CONFIRMATION);
        Offer selected = offer(load, fleet, OfferStatus.SELECTED);
        Offer other = offer(load, otherFleet, OfferStatus.PENDING);
        when(userRepository.findById(fleet.getId())).thenReturn(Optional.of(fleet));
        when(loadRepository.findByIdForUpdate(load.getId())).thenReturn(Optional.of(load));
        when(offerRepository.findByIdWithLoadForUpdate(selected.getId())).thenReturn(Optional.of(selected));
        when(offerRepository.findByLoadIdAndStatus(load.getId(), OfferStatus.PENDING)).thenReturn(List.of(other));
        when(loadRepository.save(load)).thenReturn(load);
        when(offerRepository.save(selected)).thenReturn(selected);

        OfferResponse response = offerService.confirmAssignment(
                load.getId(),
                selected.getId(),
                new CustomUserDetails(fleet)
        );

        assertEquals(OfferStatus.CONFIRMED, response.getStatus());
        assertEquals(LoadStatus.BOOKED, load.getStatus());
        assertEquals(OfferStatus.REJECTED, other.getStatus());
        verify(offerRepository).saveAll(List.of(other));
    }

    @Test
    void selectedFleetCanDeclineAssignmentAndRepostLoad() {
        User broker = user(CompanyType.BROKER);
        User fleet = user(CompanyType.FLEET);
        Load load = load(broker, LoadStatus.AWAITING_FLEET_CONFIRMATION);
        Offer selected = offer(load, fleet, OfferStatus.SELECTED);
        when(userRepository.findById(fleet.getId())).thenReturn(Optional.of(fleet));
        when(loadRepository.findByIdForUpdate(load.getId())).thenReturn(Optional.of(load));
        when(offerRepository.findByIdWithLoadForUpdate(selected.getId())).thenReturn(Optional.of(selected));
        when(loadRepository.save(load)).thenReturn(load);
        when(offerRepository.save(selected)).thenReturn(selected);

        OfferResponse response = offerService.declineAssignment(
                load.getId(),
                selected.getId(),
                new CustomUserDetails(fleet)
        );

        assertEquals(OfferStatus.REJECTED, response.getStatus());
        assertEquals(LoadStatus.POSTED, load.getStatus());
        verify(messagingService).archiveConversation(load.getId());
    }

    @Test
    void repeatedConfirmIsIdempotentAfterBooking() {
        User broker = user(CompanyType.BROKER);
        User fleet = user(CompanyType.FLEET);
        Load load = load(broker, LoadStatus.BOOKED);
        Offer confirmed = offer(load, fleet, OfferStatus.CONFIRMED);
        when(userRepository.findById(fleet.getId())).thenReturn(Optional.of(fleet));
        when(loadRepository.findByIdForUpdate(load.getId())).thenReturn(Optional.of(load));
        when(offerRepository.findByIdWithLoadForUpdate(confirmed.getId())).thenReturn(Optional.of(confirmed));

        OfferResponse response = offerService.confirmAssignment(
                load.getId(),
                confirmed.getId(),
                new CustomUserDetails(fleet)
        );

        assertEquals(OfferStatus.CONFIRMED, response.getStatus());
        verify(offerRepository, never()).save(confirmed);
    }

    @Test
    void lateFleetConfirmReleasesSelectionAndRepostsLoad() {
        User broker = user(CompanyType.BROKER);
        User fleet = user(CompanyType.FLEET);
        Load load = load(broker, LoadStatus.AWAITING_FLEET_CONFIRMATION);
        load.setConfirmationDeadlineAt(LocalDateTime.now().minusMinutes(1));
        Offer selected = offer(load, fleet, OfferStatus.SELECTED);
        when(userRepository.findById(fleet.getId())).thenReturn(Optional.of(fleet));
        when(loadRepository.findByIdForUpdate(load.getId())).thenReturn(Optional.of(load));
        when(offerRepository.findByIdWithLoadForUpdate(selected.getId())).thenReturn(Optional.of(selected));
        when(offerRepository.save(selected)).thenReturn(selected);

        OfferResponse response = offerService.confirmAssignment(
                load.getId(),
                selected.getId(),
                new CustomUserDetails(fleet)
        );

        assertEquals(OfferStatus.REJECTED, response.getStatus());
        assertEquals(LoadStatus.POSTED, load.getStatus());
        assertNotNull(load.getOfferDeadlineAt());
        verify(messagingService).archiveConversation(load.getId());
    }

    private User user(CompanyType companyType) {
        Company company = new Company();
        company.setId(UUID.randomUUID());
        company.setLegalName(companyType + " Company");
        company.setEmail(companyType + "@example.test");
        company.setType(companyType);
        company.setVerificationStatus(CompanyVerificationStatus.APPROVED);

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setFirstName("Test");
        user.setLastName(companyType.name());
        user.setEmail(companyType + "@user.test");
        user.setPassword("encoded");
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.setPhoneVerified(true);
        user.setCompany(company);
        return user;
    }

    private Load load(User broker, LoadStatus status) {
        Load load = new Load();
        load.setId(UUID.randomUUID());
        load.setBrokerCompany(broker.getCompany());
        load.setCreatedBy(broker);
        load.setPickupCity("Austin");
        load.setPickupState("TX");
        load.setPickupDate(LocalDate.now().plusDays(1));
        load.setDeliveryCity("Atlanta");
        load.setDeliveryState("GA");
        load.setDeliveryDate(LocalDate.now().plusDays(3));
        load.setEquipmentType(EquipmentType.BOX_TRUCK_26FT);
        load.setWeight(8000);
        load.setWeightLbs(8000);
        load.setRate(BigDecimal.valueOf(2400));
        load.setStatus(status);
        return load;
    }

    private Offer offer(Load load, User fleet, OfferStatus status) {
        Offer offer = new Offer();
        offer.setId(UUID.randomUUID());
        offer.setLoad(load);
        offer.setFleetUser(fleet);
        offer.setAmount(BigDecimal.valueOf(2300));
        offer.setStatus(status);
        return offer;
    }
}
