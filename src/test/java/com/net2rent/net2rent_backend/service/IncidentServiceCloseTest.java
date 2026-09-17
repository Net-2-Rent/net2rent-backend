package com.net2rent.net2rent_backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.net2rent.net2rent_backend.exception.ConflictException;
import com.net2rent.net2rent_backend.exception.NotFoundException;
import com.net2rent.net2rent_backend.model.Account;
import com.net2rent.net2rent_backend.model.Incident;
import com.net2rent.net2rent_backend.model.Lodging;
import com.net2rent.net2rent_backend.model.enums.*;
import com.net2rent.net2rent_backend.repository.IncidentRepository;
import com.net2rent.net2rent_backend.repository.UserRepository;
import com.net2rent.net2rent_backend.security.AuthUser;
import com.net2rent.net2rent_backend.security.IncidentAccessPolicy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IncidentServiceCloseTest {

    @Mock private IncidentService incidentService;
    @Mock private IncidentAccessPolicy incidentAccessPolicy;
    @Mock private IncidentRepository incidentRepository;
    @Mock private IncidentHistoryService incidentHistoryService;
    @Mock private UserRepository userRepository;
    @Mock private IncidentChecklistService incidentChecklistService;

    private IncidentExecutionService service;

    private final Clock clock =
            Clock.fixed(Instant.parse("2026-09-02T08:00:00Z"), ZoneOffset.UTC);

    private final AuthUser coordinator =
            new AuthUser(10L, 1L, "coord@net2rent.com", "COORDINATOR");

    private Account account;
    private Lodging lodging;

    @BeforeEach
    void setUp() {
        service = new IncidentExecutionService(incidentService, incidentAccessPolicy,
                incidentRepository, incidentHistoryService, userRepository, clock, incidentChecklistService);

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

    // ---------- CU-EXE-07: close (happy path) ----------

    @Test
    void close_fromResolved_setsClosedStatusAndClosedAt_recordsHistory() {
        Incident incident = incidentWithStatus(IncidentStatus.RESOLVED);
        when(incidentService.getOwnedByAccountOr404(100L, coordinator)).thenReturn(incident);

        service.close(100L, coordinator);

        assertEquals(IncidentStatus.CLOSED, incident.getStatus());
        assertNotNull(incident.getClosedAt());

        verify(incidentHistoryService, times(1)).record(
                any(Incident.class), any(), eq(IncidentEventType.STATUS_CHANGED),
                eq("RESOLVED"), eq("CLOSED"), isNull(), any(LocalDateTime.class));
        verify(incidentRepository).save(incident);
    }

    // ---------- Estados que NO son cerrables (solo RESOLVED lo es) ----------

    @Test
    void close_whenInProgress_throwsConflict() {
        Incident incident = incidentWithStatus(IncidentStatus.IN_PROGRESS);
        when(incidentService.getOwnedByAccountOr404(100L, coordinator)).thenReturn(incident);

        assertThrows(ConflictException.class, () -> service.close(100L, coordinator));

        assertEquals(IncidentStatus.IN_PROGRESS, incident.getStatus());
        assertNull(incident.getClosedAt());
        verify(incidentHistoryService, never()).record(any(), any(), any(), any(), any(), any(), any());
        verify(incidentRepository, never()).save(any());
    }

    @Test
    void close_whenNew_throwsConflict() {
        Incident incident = incidentWithStatus(IncidentStatus.NEW);
        when(incidentService.getOwnedByAccountOr404(100L, coordinator)).thenReturn(incident);

        assertThrows(ConflictException.class, () -> service.close(100L, coordinator));

        assertEquals(IncidentStatus.NEW, incident.getStatus());
        verify(incidentHistoryService, never()).record(any(), any(), any(), any(), any(), any(), any());
        verify(incidentRepository, never()).save(any());
    }

    @Test
    void close_whenAlreadyClosed_throwsConflict() {
        Incident incident = incidentWithStatus(IncidentStatus.CLOSED);
        when(incidentService.getOwnedByAccountOr404(100L, coordinator)).thenReturn(incident);

        assertThrows(ConflictException.class, () -> service.close(100L, coordinator));

        verify(incidentHistoryService, never()).record(any(), any(), any(), any(), any(), any(), any());
        verify(incidentRepository, never()).save(any());
    }

    @Test
    void close_whenRejected_throwsConflict() {
        Incident incident = incidentWithStatus(IncidentStatus.REJECTED);
        when(incidentService.getOwnedByAccountOr404(100L, coordinator)).thenReturn(incident);

        assertThrows(ConflictException.class, () -> service.close(100L, coordinator));

        assertEquals(IncidentStatus.REJECTED, incident.getStatus());
        verify(incidentHistoryService, never()).record(any(), any(), any(), any(), any(), any(), any());
        verify(incidentRepository, never()).save(any());
    }

    // ---------- Aislamiento de cuenta ----------

    @Test
    void close_whenIncidentBelongsToAnotherAccount_throwsNotFound() {
        when(incidentService.getOwnedByAccountOr404(999L, coordinator))
                .thenThrow(new NotFoundException("Incidencia no encontrada"));

        assertThrows(NotFoundException.class, () -> service.close(999L, coordinator));

        verify(incidentHistoryService, never()).record(any(), any(), any(), any(), any(), any(), any());
        verify(incidentRepository, never()).save(any());
    }
}