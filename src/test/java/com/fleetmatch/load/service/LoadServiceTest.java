package com.fleetmatch.load.service;

import com.fleetmatch.audit.service.AuditLogService;
import com.fleetmatch.common.exception.BusinessRuleException;
import com.fleetmatch.company.entity.Company;
import com.fleetmatch.company.entity.CompanyType;
import com.fleetmatch.company.entity.CompanyVerificationStatus;
import com.fleetmatch.load.dto.CreateLoadRequest;
import com.fleetmatch.load.dto.LoadResponse;
import com.fleetmatch.load.entity.EquipmentType;
import com.fleetmatch.load.entity.Load;
import com.fleetmatch.load.entity.LoadStatus;
import com.fleetmatch.load.repository.LoadRepository;
import com.fleetmatch.notification.service.NotificationService;
import com.fleetmatch.offer.entity.Offer;
import com.fleetmatch.offer.entity.OfferStatus;
import com.fleetmatch.offer.repository.OfferRepository;
import com.fleetmatch.security.user.CustomUserDetails;
import com.fleetmatch.subscription.service.SubscriptionAccessService;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoadServiceTest {

    @Mock
    private LoadRepository loadRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OfferRepository offerRepository;
    @Mock
    private SubscriptionAccessService subscriptionAccessService;
    @Mock
    private SubscriptionValidationService subscriptionValidationService;
    @Mock
    private UserVerificationGuard userVerificationGuard;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private LoadService loadService;

    @Test
    void approvedBrokerCanCreateLoad() {
        User broker = user(CompanyType.BROKER);
        CreateLoadRequest request = createLoadRequest();
        when(userRepository.findById(broker.getId())).thenReturn(Optional.of(broker));
        when(loadRepository.save(any(Load.class))).thenAnswer(invocation -> {
            Load load = invocation.getArgument(0);
            load.setId(UUID.randomUUID());
            return load;
        });
        when(subscriptionAccessService.canViewContactInfo(broker.getCompany().getId())).thenReturn(false);

        LoadResponse response = loadService.createLoad(request, new CustomUserDetails(broker));

        assertEquals(LoadStatus.POSTED, response.getStatus());
        assertEquals("Austin", response.getPickupCity());
        verify(subscriptionValidationService).validateMonthlyLoadLimit(broker.getCompany());
        verify(loadRepository).save(any(Load.class));
    }

    @Test
    void createLoadRequiresWeight() {
        User broker = user(CompanyType.BROKER);
        CreateLoadRequest request = createLoadRequest();
        request.setWeight(null);
        request.setWeightLbs(null);
        when(userRepository.findById(broker.getId())).thenReturn(Optional.of(broker));

        assertThrows(
                BusinessRuleException.class,
                () -> loadService.createLoad(request, new CustomUserDetails(broker))
        );
    }

    @Test
    void searchLoadsAppliesSubscriptionVisibilityLimit() {
        User fleet = user(CompanyType.FLEET);
        Load first = load(user(CompanyType.BROKER), LoadStatus.POSTED);
        Load second = load(user(CompanyType.BROKER), LoadStatus.POSTED);
        when(userRepository.findById(fleet.getId())).thenReturn(Optional.of(fleet));
        when(loadRepository.findByStatus(LoadStatus.POSTED)).thenReturn(List.of(first, second));
        when(subscriptionAccessService.getLoadLimit(fleet.getCompany().getId())).thenReturn(1);
        when(subscriptionAccessService.canViewContactInfo(fleet.getCompany().getId())).thenReturn(false);

        List<LoadResponse> responses = loadService.searchLoads(null, null, null, new CustomUserDetails(fleet));

        assertEquals(1, responses.size());
    }

    @Test
    void startLoadMovesBookedLoadIntoTransitForConfirmedFleet() {
        User fleet = user(CompanyType.FLEET);
        Load load = load(user(CompanyType.BROKER), LoadStatus.BOOKED);
        Offer confirmed = offer(load, fleet, OfferStatus.CONFIRMED);
        when(userRepository.findById(fleet.getId())).thenReturn(Optional.of(fleet));
        when(loadRepository.findByIdForUpdate(load.getId())).thenReturn(Optional.of(load));
        when(offerRepository.findFirstByLoadIdAndStatus(load.getId(), OfferStatus.CONFIRMED))
                .thenReturn(Optional.of(confirmed));
        when(loadRepository.save(load)).thenReturn(load);
        when(subscriptionAccessService.canViewContactInfo(fleet.getCompany().getId())).thenReturn(false);

        LoadResponse response = loadService.startLoad(load.getId(), new CustomUserDetails(fleet));

        assertEquals(LoadStatus.IN_TRANSIT, response.getStatus());
        verify(loadRepository).save(load);
    }

    @Test
    void deliveredLoadCannotBeCancelled() {
        User broker = user(CompanyType.BROKER);
        Load load = load(broker, LoadStatus.DELIVERED);
        when(userRepository.findById(broker.getId())).thenReturn(Optional.of(broker));
        when(loadRepository.findByIdForUpdate(load.getId())).thenReturn(Optional.of(load));

        assertThrows(
                BusinessRuleException.class,
                () -> loadService.cancelLoad(load.getId(), new CustomUserDetails(broker))
        );
    }

    private CreateLoadRequest createLoadRequest() {
        CreateLoadRequest request = new CreateLoadRequest();
        request.setPickupCity("Austin");
        request.setPickupState("TX");
        request.setPickupDate(LocalDate.now().plusDays(1));
        request.setDeliveryCity("Atlanta");
        request.setDeliveryState("GA");
        request.setDeliveryDate(LocalDate.now().plusDays(3));
        request.setEquipmentType(EquipmentType.BOX_TRUCK_26FT);
        request.setWeightLbs(8000);
        request.setRate(BigDecimal.valueOf(2400));
        request.setMiles(950);
        request.setCommodity("Retail fixtures");
        request.setReferenceNumber("LOAD-1");
        request.setDescription("Test load");
        return request;
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
