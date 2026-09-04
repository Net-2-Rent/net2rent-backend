package com.net2rent.net2rent_backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.net2rent.net2rent_backend.dto.IncidentResponse;
import com.net2rent.net2rent_backend.dto.request.CreateGuestIncidentRequest;
import com.net2rent.net2rent_backend.dto.request.CreatePhoneIncidentRequest;
import com.net2rent.net2rent_backend.dto.response.GuestIncidentResponse;
import com.net2rent.net2rent_backend.exception.ConflictException;
import com.net2rent.net2rent_backend.exception.NotFoundException;
import com.net2rent.net2rent_backend.model.*;
import com.net2rent.net2rent_backend.model.enums.IncidentCategory;
import com.net2rent.net2rent_backend.model.enums.IncidentPriority;
import com.net2rent.net2rent_backend.model.enums.IncidentSource;
import com.net2rent.net2rent_backend.model.enums.IncidentStatus;
import com.net2rent.net2rent_backend.model.enums.UserRole;
import com.net2rent.net2rent_backend.repository.IncidentCounterRepository;
import com.net2rent.net2rent_backend.repository.IncidentHistoryRepository;
import com.net2rent.net2rent_backend.repository.IncidentRepository;
import com.net2rent.net2rent_backend.repository.LodgingRepository;
import com.net2rent.net2rent_backend.repository.UserRepository;
import com.net2rent.net2rent_backend.security.AuthUser;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import com.net2rent.net2rent_backend.security.GuestPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock private IncidentRepository incidentRepository;
    @Mock private IncidentCounterRepository incidentCounterRepository;
    @Mock private IncidentHistoryRepository incidentHistoryRepository;
    @Mock private LodgingRepository lodgingRepository;
    @Mock private UserRepository userRepository;

    private IncidentService service;

    private final Clock clock =
            Clock.fixed(Instant.parse("2026-09-02T08:00:00Z"), ZoneOffset.UTC);

    private final AuthUser coordinator =
            new AuthUser(10L, 1L, "coord@net2rent.com", "COORDINATOR");

    private Account account;
    private Lodging activeLodging;

    @BeforeEach
    void setUp() {
        service = new IncidentService(
                incidentRepository, incidentCounterRepository, incidentHistoryRepository,
                lodgingRepository, userRepository, clock);

        account = Account.builder().id(1L).name("net2Rent Demo").build();
        activeLodging = Lodging.builder()
                .id(1L).account(account).ref("APT-1001").name("Piso Centro").active(true).build();
    }

    private CreatePhoneIncidentRequest request(Long assigneeId) {
        return new CreatePhoneIncidentRequest(
                1L,
                LocalDateTime.of(2026, 9, 1, 9, 0),
                "Ana", "López",
                null,
                IncidentCategory.ELECTRICITY,
                IncidentPriority.NORMAL,
                assigneeId,
                "No hay luz en el salón desde ayer");
    }

    @Test
    void withoutOperator_createsNew_withOneHistoryEvent() {
        when(lodgingRepository.findByIdAndAccount_Id(1L, 1L)).thenReturn(Optional.of(activeLodging));
        when(incidentCounterRepository.findForUpdate(1L, 2026)).thenReturn(Optional.of(
                IncidentCounter.builder().account(account).year(2026).lastNumber(0).build()));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(inv -> inv.getArgument(0));

        IncidentResponse res = service.registerPhoneIncident(request(null), coordinator);

        assertEquals("NEW", res.status());
        assertEquals("INC-2026-000001", res.code());

        ArgumentCaptor<Incident> captor = ArgumentCaptor.forClass(Incident.class);
        verify(incidentRepository).save(captor.capture());
        Incident saved = captor.getValue();
        assertEquals(IncidentStatus.NEW, saved.getStatus());
        assertEquals(IncidentSource.PHONE, saved.getSource());
        assertNull(saved.getAssignee());
        assertNull(saved.getAssignedAt());
        assertEquals(LocalDateTime.of(2026, 9, 2, 8, 0), saved.getCreatedAt());
        assertEquals(LocalDateTime.of(2026, 9, 1, 9, 0), saved.getOpenedAt());
        assertEquals("No hay luz en el salón desde ayer", saved.getTitle());

        verify(incidentHistoryRepository, times(1)).save(any());
    }

    @Test
    void withOperator_createsAssigned_withTwoHistoryEvents() {
        AppUser operator = AppUser.builder()
                .id(2L).account(account).firstName("Marta").lastName("Ruiz")
                .role(UserRole.OPERATOR).active(true).build();

        when(lodgingRepository.findByIdAndAccount_Id(1L, 1L)).thenReturn(Optional.of(activeLodging));
        when(userRepository.findByIdAndAccount_Id(2L, 1L)).thenReturn(Optional.of(operator));
        when(incidentCounterRepository.findForUpdate(1L, 2026)).thenReturn(Optional.of(
                IncidentCounter.builder().account(account).year(2026).lastNumber(0).build()));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(inv -> inv.getArgument(0));

        IncidentResponse res = service.registerPhoneIncident(request(2L), coordinator);

        assertEquals("ASSIGNED", res.status());

        ArgumentCaptor<Incident> captor = ArgumentCaptor.forClass(Incident.class);
        verify(incidentRepository).save(captor.capture());
        Incident saved = captor.getValue();
        assertEquals(IncidentStatus.ASSIGNED, saved.getStatus());
        assertNotNull(saved.getAssignee());
        assertEquals(LocalDateTime.of(2026, 9, 2, 8, 0), saved.getAssignedAt());

        verify(incidentHistoryRepository, times(2)).save(any());
    }

    @Test
    void lodgingOfAnotherAccountOrMissing_throwsNotFound() {
        when(lodgingRepository.findByIdAndAccount_Id(1L, 1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.registerPhoneIncident(request(null), coordinator));

        verify(incidentRepository, never()).save(any());
    }

    @Test
    void inactiveLodging_throwsConflict() {
        Lodging inactive = Lodging.builder()
                .id(1L).account(account).ref("APT-1001").name("Piso Centro").active(false).build();
        when(lodgingRepository.findByIdAndAccount_Id(1L, 1L)).thenReturn(Optional.of(inactive));

        assertThrows(ConflictException.class,
                () -> service.registerPhoneIncident(request(null), coordinator));

        verify(incidentRepository, never()).save(any());
    }

    @Test
    void invalidOperator_throwsConflict() {
        AppUser inactiveOperator = AppUser.builder()
                .id(2L).account(account).firstName("Marta").lastName("Ruiz")
                .role(UserRole.OPERATOR).active(false).build();
        when(lodgingRepository.findByIdAndAccount_Id(1L, 1L)).thenReturn(Optional.of(activeLodging));
        when(userRepository.findByIdAndAccount_Id(2L, 1L)).thenReturn(Optional.of(inactiveOperator));

        assertThrows(ConflictException.class,
                () -> service.registerPhoneIncident(request(2L), coordinator));

        verify(incidentRepository, never()).save(any());
    }

    @Test
    void guestIncident_createsNew_withNullActorHistoryEvent() {
        when(lodgingRepository.findById(1L)).thenReturn(Optional.of(activeLodging));
        when(incidentCounterRepository.findForUpdate(1L, 2026)).thenReturn(Optional.of(
                IncidentCounter.builder().account(account).year(2026).lastNumber(0).build()));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(inv -> inv.getArgument(0));

        GuestPrincipal guest = new GuestPrincipal(1L);
        CreateGuestIncidentRequest req = new CreateGuestIncidentRequest(
                "Ana", "López", null, IncidentCategory.ELECTRICITY, "No hay luz en el salón desde ayer");

        GuestIncidentResponse res = service.registerGuestIncident(req, guest);

        assertEquals("INC-2026-000001", res.code());

        ArgumentCaptor<Incident> incidentCaptor = ArgumentCaptor.forClass(Incident.class);
        verify(incidentRepository).save(incidentCaptor.capture());
        Incident saved = incidentCaptor.getValue();
        assertEquals(IncidentStatus.NEW, saved.getStatus());
        assertEquals(IncidentSource.GUEST_PORTAL, saved.getSource());
        assertEquals(IncidentPriority.NORMAL, saved.getPriority());
        assertEquals(IncidentCategory.ELECTRICITY, saved.getCategory());
        assertNull(saved.getAssignee());

        ArgumentCaptor<IncidentHistory> historyCaptor = ArgumentCaptor.forClass(IncidentHistory.class);
        verify(incidentHistoryRepository, times(1)).save(historyCaptor.capture());
        assertNull(historyCaptor.getValue().getActor());
    }

    @Test
    void guestIncident_withoutCategory_leavesCategoryNull() {
        when(lodgingRepository.findById(1L)).thenReturn(Optional.of(activeLodging));
        when(incidentCounterRepository.findForUpdate(1L, 2026)).thenReturn(Optional.of(
                IncidentCounter.builder().account(account).year(2026).lastNumber(0).build()));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(inv -> inv.getArgument(0));

        GuestPrincipal guest = new GuestPrincipal(1L);
        CreateGuestIncidentRequest req = new CreateGuestIncidentRequest(
                "Ana", "López", null, null, "No hay luz en el salón desde ayer");

        service.registerGuestIncident(req, guest);

        ArgumentCaptor<Incident> captor = ArgumentCaptor.forClass(Incident.class);
        verify(incidentRepository).save(captor.capture());
        assertNull(captor.getValue().getCategory());
    }

    @Test
    void guestIncident_missingLodging_throwsNotFound() {
        when(lodgingRepository.findById(1L)).thenReturn(Optional.empty());

        GuestPrincipal guest = new GuestPrincipal(1L);
        CreateGuestIncidentRequest req = new CreateGuestIncidentRequest(
                "Ana", "López", null, null, "No hay luz en el salón desde ayer");

        assertThrows(NotFoundException.class, () -> service.registerGuestIncident(req, guest));

        verify(incidentRepository, never()).save(any());
    }

    @Test
    void guestIncident_inactiveLodging_throwsNotFound() {
        Lodging inactive = Lodging.builder()
                .id(1L).account(account).ref("APT-1001").name("Piso Centro").active(false).build();
        when(lodgingRepository.findById(1L)).thenReturn(Optional.of(inactive));

        GuestPrincipal guest = new GuestPrincipal(1L);
        CreateGuestIncidentRequest req = new CreateGuestIncidentRequest(
                "Ana", "López", null, null, "No hay luz en el salón desde ayer");

        assertThrows(NotFoundException.class, () -> service.registerGuestIncident(req, guest));

        verify(incidentRepository, never()).save(any());
    }
}