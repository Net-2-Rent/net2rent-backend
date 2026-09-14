package com.net2rent.net2rent_backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.net2rent.net2rent_backend.dto.AssignOperatorRequest;
import com.net2rent.net2rent_backend.exception.ConflictException;
import com.net2rent.net2rent_backend.model.Account;
import com.net2rent.net2rent_backend.model.AppUser;
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

// NET-69: asignar y reasignar operario (CU-INC-06, 07, 11).
@ExtendWith(MockitoExtension.class)
class IncidentServiceAssignTest {

    @Mock private IncidentService incidentService;
    @Mock private IncidentAccessPolicy incidentAccessPolicy;
    @Mock private IncidentRepository incidentRepository;
    @Mock private IncidentHistoryService incidentHistoryService;
    @Mock private UserRepository userRepository;
    @Mock private IncidentChecklistService incidentChecklistService;

    private IncidentExecutionService service;

    private final Clock clock =
            Clock.fixed(Instant.parse("2026-09-10T08:00:00Z"), ZoneOffset.UTC);

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

    private AppUser operator(Long id, String firstName) {
        return AppUser.builder()
                .id(id).account(account)
                .firstName(firstName).lastName("Demo")
                .role(UserRole.OPERATOR).active(true).build();
    }

    private Incident incident(IncidentStatus status, AppUser assignee) {
        return Incident.builder()
                .id(100L).account(account).code("INC-2026-000001")
                .status(status).priority(IncidentPriority.NORMAL)
                .category(IncidentCategory.PLUMBING).lodging(lodging)
                .title("Fuga").description("Fuga")
                .assignee(assignee)
                .openedAt(LocalDateTime.of(2026, 9, 1, 9, 0))
                .createdAt(LocalDateTime.of(2026, 9, 1, 9, 0))
                .build();
    }

    // ---------- CU-INC-06: asignar ----------

    @Test
    void assign_toNewIncident_setsAssignedAndRecordsHistory() {
        Incident inc = incident(IncidentStatus.NEW, null);
        AppUser op = operator(3L, "Operario");
        when(incidentService.getOwnedByAccountOr404(100L, coordinator)).thenReturn(inc);
        when(incidentService.validateOperator(3L, 1L)).thenReturn(op);

        service.assignOperator(100L, new AssignOperatorRequest(3L, null), coordinator);

        assertEquals(IncidentStatus.ASSIGNED, inc.getStatus());
        assertEquals(op, inc.getAssignee());
        verify(incidentHistoryService).record(eq(inc), any(), eq(IncidentEventType.ASSIGNED),
                isNull(), eq("3"), isNull(), any(LocalDateTime.class));
        verify(incidentRepository).save(inc);
    }

    // ---------- CU-INC-07: reasignar ----------

    @Test
    void reassign_fromInProgress_backToAssigned_recordsReassignAndStatusChange() {
        AppUser current = operator(3L, "Operario");
        Incident inc = incident(IncidentStatus.IN_PROGRESS, current);
        AppUser next = operator(4L, "Operario2");
        when(incidentService.getOwnedByAccountOr404(100L, coordinator)).thenReturn(inc);
        when(incidentService.validateOperator(4L, 1L)).thenReturn(next);

        service.assignOperator(100L, new AssignOperatorRequest(4L, "Reparto"), coordinator);

        assertEquals(next, inc.getAssignee());
        assertEquals(IncidentStatus.ASSIGNED, inc.getStatus());

        verify(incidentHistoryService).record(eq(inc), any(), eq(IncidentEventType.REASSIGNED),
                eq("3"), eq("4"), eq("Reparto"), any(LocalDateTime.class));
        verify(incidentHistoryService).record(eq(inc), any(), eq(IncidentEventType.STATUS_CHANGED),
                eq("IN_PROGRESS"), eq("ASSIGNED"), isNull(), any(LocalDateTime.class));
    }

    @Test
    void reassign_withoutReason_throwsConflict() {
        AppUser current = operator(3L, "Operario");
        Incident inc = incident(IncidentStatus.ASSIGNED, current);
        AppUser next = operator(4L, "Operario2");
        when(incidentService.getOwnedByAccountOr404(100L, coordinator)).thenReturn(inc);
        when(incidentService.validateOperator(4L, 1L)).thenReturn(next);

        assertThrows(ConflictException.class, () ->
                service.assignOperator(100L, new AssignOperatorRequest(4L, "   "), coordinator));

        verify(incidentRepository, never()).save(any());
    }

    @Test
    void reassign_toSameOperator_throwsConflict() {
        AppUser current = operator(3L, "Operario");
        Incident inc = incident(IncidentStatus.ASSIGNED, current);
        when(incidentService.getOwnedByAccountOr404(100L, coordinator)).thenReturn(inc);
        when(incidentService.validateOperator(3L, 1L)).thenReturn(current);

        assertThrows(ConflictException.class, () ->
                service.assignOperator(100L, new AssignOperatorRequest(3L, "motivo"), coordinator));

        verify(incidentRepository, never()).save(any());
    }

    // ---------- CU-INC-11: operario inválido ----------

    @Test
    void assign_invalidOperator_throwsConflict() {
        Incident inc = incident(IncidentStatus.NEW, null);
        when(incidentService.getOwnedByAccountOr404(100L, coordinator)).thenReturn(inc);
        when(incidentService.validateOperator(99L, 1L))
                .thenThrow(new ConflictException("Operario no válido"));

        assertThrows(ConflictException.class, () ->
                service.assignOperator(100L, new AssignOperatorRequest(99L, null), coordinator));

        verify(incidentRepository, never()).save(any());
    }

    // ---------- Máquina de estados ----------

    @Test
    void assign_whenClosed_throwsConflict() {
        Incident inc = incident(IncidentStatus.CLOSED, null);
        when(incidentService.getOwnedByAccountOr404(100L, coordinator)).thenReturn(inc);
        doThrow(new ConflictException("La incidencia está cerrada"))
                .when(incidentAccessPolicy).ensureNotTerminal(inc);

        assertThrows(ConflictException.class, () ->
                service.assignOperator(100L, new AssignOperatorRequest(3L, null), coordinator));

        verify(incidentRepository, never()).save(any());
    }

    @Test
    void assign_whenResolved_throwsConflict() {
        Incident inc = incident(IncidentStatus.RESOLVED, null);
        when(incidentService.getOwnedByAccountOr404(100L, coordinator)).thenReturn(inc);

        assertThrows(ConflictException.class, () ->
                service.assignOperator(100L, new AssignOperatorRequest(3L, null), coordinator));

        verify(incidentRepository, never()).save(any());
    }
}