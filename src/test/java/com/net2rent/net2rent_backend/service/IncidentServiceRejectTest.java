package com.net2rent.net2rent_backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.net2rent.net2rent_backend.dto.RejectIncidentRequest;
import com.net2rent.net2rent_backend.exception.ConflictException;
import com.net2rent.net2rent_backend.exception.NotFoundException;
import com.net2rent.net2rent_backend.model.Account;
import com.net2rent.net2rent_backend.model.Incident;
import com.net2rent.net2rent_backend.model.Lodging;
import com.net2rent.net2rent_backend.model.enums.*;
import com.net2rent.net2rent_backend.repository.IncidentCounterRepository;
import com.net2rent.net2rent_backend.repository.IncidentRepository;
import com.net2rent.net2rent_backend.repository.LodgingRepository;
import com.net2rent.net2rent_backend.repository.UserRepository;
import com.net2rent.net2rent_backend.security.AuthUser;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IncidentServiceRejectTest {

    @Mock private IncidentRepository incidentRepository;
    @Mock private IncidentCounterRepository incidentCounterRepository;
    @Mock private IncidentHistoryService incidentHistoryService;
    @Mock private LodgingRepository lodgingRepository;
    @Mock private UserRepository userRepository;
    @Mock private IncidentImageService incidentImageService;

    private IncidentService service;

    private final Clock clock =
            Clock.fixed(Instant.parse("2026-09-02T08:00:00Z"), ZoneOffset.UTC);

    private final AuthUser coordinator =
            new AuthUser(10L, 1L, "coord@net2rent.com", "COORDINATOR");

    private Account account;
    private Lodging lodging;

    @BeforeEach
    void setUp() {
        service = new IncidentService(
                incidentRepository, incidentCounterRepository, incidentHistoryService,
                lodgingRepository, userRepository, incidentImageService, clock);

        account = Account.builder().id(1L).name("net2Rent Demo").build();
        lodging = Lodging.builder()
                .id(1L).account(account).ref("APT-1001").name("Piso Centro").active(true).build();
    }

    private Incident incidentWithStatus(IncidentStatus status) {
        return Incident.builder()
                .id(100L)
                .account(account)
                .code("INC-2026-000001")
                .status(status)
                .priority(IncidentPriority.NORMAL)
                .lodging(lodging)
                .title("Fuga de agua en el baño")
                .description("Fuga de agua en el baño")
                .guestFirstName("Ana").guestLastName("López")
                .openedAt(LocalDateTime.of(2026, 9, 1, 9, 0))
                .createdAt(LocalDateTime.of(2026, 9, 1, 9, 0))
                .build();
    }

    // ---------- CU-INC-08: reject (happy path) ----------

    @Test
    void reject_fromNew_setsRejectedStatusAndReason_recordsHistory() {
        Incident incident = incidentWithStatus(IncidentStatus.NEW);
        when(incidentRepository.findByIdAndAccount_Id(100L, 1L)).thenReturn(Optional.of(incident));

        service.reject(100L, new RejectIncidentRequest("  Duplicada  "), coordinator);

        assertEquals(IncidentStatus.REJECTED, incident.getStatus());
        assertEquals("Duplicada", incident.getRejectionReason());

        verify(incidentHistoryService, times(1)).record(
                any(Incident.class), any(), eq(IncidentEventType.STATUS_CHANGED),
                eq("NEW"), eq("REJECTED"), any(LocalDateTime.class));
        verify(incidentRepository).save(incident);
    }

    @Test
    void reject_fromInProgress_isAllowed() {
        Incident incident = incidentWithStatus(IncidentStatus.IN_PROGRESS);
        when(incidentRepository.findByIdAndAccount_Id(100L, 1L)).thenReturn(Optional.of(incident));

        service.reject(100L, new RejectIncidentRequest("No procede"), coordinator);

        assertEquals(IncidentStatus.REJECTED, incident.getStatus());
        verify(incidentHistoryService, times(1)).record(
                any(Incident.class), any(), eq(IncidentEventType.STATUS_CHANGED),
                eq("IN_PROGRESS"), eq("REJECTED"), any(LocalDateTime.class));
    }

    // ---------- Status that are not rejectables ----------

    @Test
    void reject_whenAlreadyClosed_throwsConflict() {
        Incident incident = incidentWithStatus(IncidentStatus.CLOSED);
        when(incidentRepository.findByIdAndAccount_Id(100L, 1L)).thenReturn(Optional.of(incident));

        assertThrows(ConflictException.class, () ->
                service.reject(100L, new RejectIncidentRequest("Duplicada"), coordinator));

        // No cambia nada ni deja rastro
        assertEquals(IncidentStatus.CLOSED, incident.getStatus());
        verify(incidentHistoryService, never()).record(any(), any(), any(), any(), any(), any());
        verify(incidentRepository, never()).save(any());
    }

    @Test
    void reject_whenAlreadyRejected_throwsConflict() {
        Incident incident = incidentWithStatus(IncidentStatus.REJECTED);
        when(incidentRepository.findByIdAndAccount_Id(100L, 1L)).thenReturn(Optional.of(incident));

        assertThrows(ConflictException.class, () ->
                service.reject(100L, new RejectIncidentRequest("Duplicada"), coordinator));

        verify(incidentHistoryService, never()).record(any(), any(), any(), any(), any(), any());
        verify(incidentRepository, never()).save(any());
    }

    @Test
    void reject_whenResolved_throwsConflict() {
        Incident incident = incidentWithStatus(IncidentStatus.RESOLVED);
        when(incidentRepository.findByIdAndAccount_Id(100L, 1L)).thenReturn(Optional.of(incident));

        assertThrows(ConflictException.class, () ->
                service.reject(100L, new RejectIncidentRequest("Duplicada"), coordinator));

        assertEquals(IncidentStatus.RESOLVED, incident.getStatus());
        verify(incidentHistoryService, never()).record(any(), any(), any(), any(), any(), any());
    }

    // ---------- Account aisolation ----------

    @Test
    void reject_whenIncidentBelongsToAnotherAccount_throwsNotFound() {
        when(incidentRepository.findByIdAndAccount_Id(999L, 1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                service.reject(999L, new RejectIncidentRequest("Duplicada"), coordinator));

        verify(incidentHistoryService, never()).record(any(), any(), any(), any(), any(), any());
    }
}