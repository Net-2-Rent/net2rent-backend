package com.net2rent.net2rent_backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.net2rent.net2rent_backend.dto.IncidentResponse;
import com.net2rent.net2rent_backend.dto.PauseIncidentRequest;
import com.net2rent.net2rent_backend.dto.ResolveIncidentRequest;
import com.net2rent.net2rent_backend.exception.ConflictException;
import com.net2rent.net2rent_backend.exception.ForbiddenException;
import com.net2rent.net2rent_backend.exception.NotFoundException;
import com.net2rent.net2rent_backend.model.AppUser;
import com.net2rent.net2rent_backend.model.Incident;
import com.net2rent.net2rent_backend.model.enums.IncidentEventType;
import com.net2rent.net2rent_backend.model.enums.IncidentStatus;
import com.net2rent.net2rent_backend.model.enums.IncidentCategory;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IncidentExecutionServiceTest {

    @Mock private IncidentService incidentService;
    @Mock private IncidentAccessPolicy incidentAccessPolicy;
    @Mock private IncidentRepository incidentRepository;
    @Mock private IncidentHistoryService incidentHistoryService;
    @Mock private IncidentChecklistService incidentChecklistService;
    @Mock private UserRepository userRepository;

    private final Clock clock =
            Clock.fixed(Instant.parse("2026-09-08T08:00:00Z"), ZoneOffset.UTC);

    private IncidentExecutionService service;

    private final AuthUser coordinator = new AuthUser(10L, 1L, "coord@net2rent.com", "COORDINATOR");
    private final AuthUser operator    = new AuthUser(20L, 1L, "op@net2rent.com", "OPERATOR");

    @BeforeEach
    void setUp() {
        service = new IncidentExecutionService(incidentService, incidentAccessPolicy,
                incidentRepository, incidentHistoryService, userRepository, clock, incidentChecklistService);
    }
    private Incident incidentWithStatus(IncidentStatus status) {
        return Incident.builder().id(5L).status(status).build();
    }

    // ---------- start (CU-EXE-03) ----------

    @Test
    void start_fromAssigned_setsInProgressAndStartedAt_recordsHistory() {
        Incident incident = incidentWithStatus(IncidentStatus.ASSIGNED);
        AppUser actor = AppUser.builder().id(10L).build();

        when(incidentService.getOwnedByAccountOr404(5L, coordinator)).thenReturn(incident);
        when(userRepository.getReferenceById(10L)).thenReturn(actor);

        IncidentResponse response = service.start(5L, coordinator);

        assertEquals(IncidentStatus.IN_PROGRESS, incident.getStatus());
        assertEquals(LocalDateTime.of(2026, 9, 8, 8, 0), incident.getStartedAt());
        assertEquals("IN_PROGRESS", response.status());

        verify(incidentAccessPolicy).ensureCanActOn(incident, coordinator);
        verify(incidentHistoryService).record(
                same(incident), same(actor), eq(IncidentEventType.STATUS_CHANGED),
                eq("ASSIGNED"), eq("IN_PROGRESS"), any(LocalDateTime.class));
        verify(incidentRepository).save(incident);
    }

    @ParameterizedTest
    @EnumSource(value = IncidentStatus.class, names = {"ASSIGNED"}, mode = EnumSource.Mode.EXCLUDE)
    void start_whenNotAssigned_throwsConflict_andDoesNotSave(IncidentStatus otherStatus) {
        Incident incident = incidentWithStatus(otherStatus);
        when(incidentService.getOwnedByAccountOr404(5L, coordinator)).thenReturn(incident);

        assertThrows(ConflictException.class, () -> service.start(5L, coordinator));

        assertEquals(otherStatus, incident.getStatus());
        verify(incidentHistoryService, never()).record(any(), any(), any(), any(), any(), any());
        verify(incidentRepository, never()).save(any());
    }

    @Test
    void start_whenPolicyForbids_throws403_andDoesNotSave() {
        Incident incident = incidentWithStatus(IncidentStatus.ASSIGNED);
        when(incidentService.getOwnedByAccountOr404(5L, operator)).thenReturn(incident);
        doThrow(new ForbiddenException("No puedes editar una incidencia que no tienes asignada"))
                .when(incidentAccessPolicy).ensureCanActOn(incident, operator);

        assertThrows(ForbiddenException.class, () -> service.start(5L, operator));

        verify(incidentHistoryService, never()).record(any(), any(), any(), any(), any(), any());
        verify(incidentRepository, never()).save(any());
    }

    @Test
    void start_whenIncidentNotAccessible_propagates404() {
        when(incidentService.getOwnedByAccountOr404(99L, coordinator))
                .thenThrow(new NotFoundException("Incidencia no encontrada"));

        assertThrows(NotFoundException.class, () -> service.start(99L, coordinator));
        verifyNoInteractions(incidentAccessPolicy, incidentRepository, incidentHistoryService);
    }

    // ---------- pause (CU-EXE-04) ----------

    @Test
    void pause_fromInProgress_setsPausedAndReason_recordsHistory() {
        Incident incident = incidentWithStatus(IncidentStatus.IN_PROGRESS);
        AppUser actor = AppUser.builder().id(10L).build();

        when(incidentService.getOwnedByAccountOr404(5L, coordinator)).thenReturn(incident);
        when(userRepository.getReferenceById(10L)).thenReturn(actor);

        IncidentResponse response =
                service.pause(5L, new PauseIncidentRequest("  Falta material  "), coordinator);

        assertEquals(IncidentStatus.PAUSED, incident.getStatus());
        assertEquals("Falta material", incident.getPauseReason());
        assertEquals("PAUSED", response.status());
        assertEquals("Falta material", response.pauseReason());

        verify(incidentHistoryService).record(
                same(incident), same(actor), eq(IncidentEventType.STATUS_CHANGED),
                eq("IN_PROGRESS"), eq("PAUSED"), any(LocalDateTime.class));
        verify(incidentRepository).save(incident);
    }

    @ParameterizedTest
    @EnumSource(value = IncidentStatus.class, names = {"IN_PROGRESS"}, mode = EnumSource.Mode.EXCLUDE)
    void pause_whenNotInProgress_throwsConflict_andDoesNotSave(IncidentStatus otherStatus) {
        Incident incident = incidentWithStatus(otherStatus);
        when(incidentService.getOwnedByAccountOr404(5L, coordinator)).thenReturn(incident);

        assertThrows(ConflictException.class,
                () -> service.pause(5L, new PauseIncidentRequest("Falta material"), coordinator));

        verify(incidentHistoryService, never()).record(any(), any(), any(), any(), any(), any());
        verify(incidentRepository, never()).save(any());
    }

    @Test
    void pause_whenPolicyForbids_throws403_andDoesNotSave() {
        Incident incident = incidentWithStatus(IncidentStatus.IN_PROGRESS);
        when(incidentService.getOwnedByAccountOr404(5L, operator)).thenReturn(incident);
        doThrow(new ForbiddenException("No puedes editar una incidencia que no tienes asignada"))
                .when(incidentAccessPolicy).ensureCanActOn(incident, operator);

        assertThrows(ForbiddenException.class,
                () -> service.pause(5L, new PauseIncidentRequest("Falta material"), operator));

        verify(incidentHistoryService, never()).record(any(), any(), any(), any(), any(), any());
        verify(incidentRepository, never()).save(any());
    }

    @Test
    void pause_whenIncidentNotAccessible_propagates404() {
        when(incidentService.getOwnedByAccountOr404(99L, coordinator))
                .thenThrow(new NotFoundException("Incidencia no encontrada"));

        assertThrows(NotFoundException.class,
                () -> service.pause(99L, new PauseIncidentRequest("Falta material"), coordinator));
        verifyNoInteractions(incidentAccessPolicy, incidentRepository, incidentHistoryService);
    }

    // ---------- resume (CU-EXE-05) ----------

    @Test
    void resume_fromPaused_setsInProgress_recordsHistory() {
        Incident incident = incidentWithStatus(IncidentStatus.PAUSED);
        AppUser actor = AppUser.builder().id(10L).build();

        when(incidentService.getOwnedByAccountOr404(5L, coordinator)).thenReturn(incident);
        when(userRepository.getReferenceById(10L)).thenReturn(actor);

        IncidentResponse response = service.resume(5L, coordinator);

        assertEquals(IncidentStatus.IN_PROGRESS, incident.getStatus());
        assertEquals("IN_PROGRESS", response.status());

        verify(incidentHistoryService).record(
                same(incident), same(actor), eq(IncidentEventType.STATUS_CHANGED),
                eq("PAUSED"), eq("IN_PROGRESS"), any(LocalDateTime.class));
        verify(incidentRepository).save(incident);
    }

    @ParameterizedTest
    @EnumSource(value = IncidentStatus.class, names = {"PAUSED"}, mode = EnumSource.Mode.EXCLUDE)
    void resume_whenNotPaused_throwsConflict_andDoesNotSave(IncidentStatus otherStatus) {
        Incident incident = incidentWithStatus(otherStatus);
        when(incidentService.getOwnedByAccountOr404(5L, coordinator)).thenReturn(incident);

        assertThrows(ConflictException.class, () -> service.resume(5L, coordinator));

        verify(incidentHistoryService, never()).record(any(), any(), any(), any(), any(), any());
        verify(incidentRepository, never()).save(any());
    }

    @Test
    void resume_whenPolicyForbids_throws403_andDoesNotSave() {
        Incident incident = incidentWithStatus(IncidentStatus.PAUSED);
        when(incidentService.getOwnedByAccountOr404(5L, operator)).thenReturn(incident);
        doThrow(new ForbiddenException("No puedes editar una incidencia que no tienes asignada"))
                .when(incidentAccessPolicy).ensureCanActOn(incident, operator);

        assertThrows(ForbiddenException.class, () -> service.resume(5L, operator));

        verify(incidentHistoryService, never()).record(any(), any(), any(), any(), any(), any());
        verify(incidentRepository, never()).save(any());
    }

    @Test
    void resume_whenIncidentNotAccessible_propagates404() {
        when(incidentService.getOwnedByAccountOr404(99L, coordinator))
                .thenThrow(new NotFoundException("Incidencia no encontrada"));

        assertThrows(NotFoundException.class, () -> service.resume(99L, coordinator));
        verifyNoInteractions(incidentAccessPolicy, incidentRepository, incidentHistoryService);
    }

    // ---------- resolve (CU-EXE-06) ----------

    @Test
    void resolve_fromInProgress_setsResolvedAndSealsData_recordsHistory() {
        Incident incident = incidentWithStatus(IncidentStatus.IN_PROGRESS);
        incident.setCategory(IncidentCategory.PLUMBING);
        AppUser actor = AppUser.builder().id(10L).build();

        when(incidentService.getOwnedByAccountOr404(5L, coordinator)).thenReturn(incident);
        when(userRepository.getReferenceById(10L)).thenReturn(actor);

        IncidentResponse response = service.resolve(5L,
                new ResolveIncidentRequest(45, "  Cambiada la resistencia del termo  "), coordinator);

        assertEquals(IncidentStatus.RESOLVED, incident.getStatus());
        assertEquals(LocalDateTime.of(2026, 9, 8, 8, 0), incident.getResolvedAt());
        assertEquals(45, incident.getMinutesSpent());
        assertEquals("Cambiada la resistencia del termo", incident.getResolutionNote());
        assertEquals("RESOLVED", response.status());

        verify(incidentAccessPolicy).ensureCanActOn(incident, coordinator);
        verify(incidentHistoryService).record(
                same(incident), same(actor), eq(IncidentEventType.STATUS_CHANGED),
                eq("IN_PROGRESS"), eq("RESOLVED"), any(LocalDateTime.class));
        verify(incidentHistoryService).record(
                same(incident), same(actor), eq(IncidentEventType.TIME_LOGGED),
                isNull(), eq("45"), any(LocalDateTime.class));
        verify(incidentRepository).save(incident);
    }

    @Test
    void resolve_fromPaused_setsResolvedAndSealsData_recordsHistory() {
        Incident incident = incidentWithStatus(IncidentStatus.PAUSED);
        incident.setCategory(IncidentCategory.ELECTRICITY);
        AppUser actor = AppUser.builder().id(10L).build();

        when(incidentService.getOwnedByAccountOr404(5L, coordinator)).thenReturn(incident);
        when(userRepository.getReferenceById(10L)).thenReturn(actor);

        IncidentResponse response = service.resolve(5L,
                new ResolveIncidentRequest(20, "Revisado el cuadro eléctrico"), coordinator);

        assertEquals(IncidentStatus.RESOLVED, incident.getStatus());
        assertEquals("RESOLVED", response.status());
        verify(incidentHistoryService).record(
                same(incident), same(actor), eq(IncidentEventType.STATUS_CHANGED),
                eq("PAUSED"), eq("RESOLVED"), any(LocalDateTime.class));
        verify(incidentRepository).save(incident);
    }

    @ParameterizedTest
    @EnumSource(value = IncidentStatus.class, names = {"IN_PROGRESS", "PAUSED"}, mode = EnumSource.Mode.EXCLUDE)
    void resolve_whenNotInProgressOrPaused_throwsConflict_andDoesNotSave(IncidentStatus otherStatus) {
        Incident incident = incidentWithStatus(otherStatus);
        when(incidentService.getOwnedByAccountOr404(5L, coordinator)).thenReturn(incident);

        ResolveIncidentRequest request = new ResolveIncidentRequest(30, "Nota");
        assertThrows(ConflictException.class, () -> service.resolve(5L, request, coordinator));

        assertEquals(otherStatus, incident.getStatus());
        verify(incidentHistoryService, never()).record(any(), any(), any(), any(), any(), any());
        verify(incidentRepository, never()).save(any());
    }

    @Test
    void resolve_whenPolicyForbids_throws403_andDoesNotSave() {
        Incident incident = incidentWithStatus(IncidentStatus.IN_PROGRESS);
        when(incidentService.getOwnedByAccountOr404(5L, operator)).thenReturn(incident);
        doThrow(new ForbiddenException("No puedes editar una incidencia que no tienes asignada"))
                .when(incidentAccessPolicy).ensureCanActOn(incident, operator);

        ResolveIncidentRequest request = new ResolveIncidentRequest(30, "Nota");
        assertThrows(ForbiddenException.class, () -> service.resolve(5L, request, operator));

        verify(incidentHistoryService, never()).record(any(), any(), any(), any(), any(), any());
        verify(incidentRepository, never()).save(any());
    }

    @Test
    void resolve_whenIncidentNotAccessible_propagates404() {
        when(incidentService.getOwnedByAccountOr404(99L, coordinator))
                .thenThrow(new NotFoundException("Incidencia no encontrada"));

        ResolveIncidentRequest request = new ResolveIncidentRequest(30, "Nota");
        assertThrows(NotFoundException.class, () -> service.resolve(99L, request, coordinator));
        verifyNoInteractions(incidentAccessPolicy, incidentRepository, incidentHistoryService, incidentChecklistService);
    }

    @Test
    void resolve_whenCategoryNull_throwsConflict_andDoesNotSave() {
        Incident incident = incidentWithStatus(IncidentStatus.IN_PROGRESS); // sin categoría

        when(incidentService.getOwnedByAccountOr404(5L, coordinator)).thenReturn(incident);

        ResolveIncidentRequest request = new ResolveIncidentRequest(30, "Nota");
        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.resolve(5L, request, coordinator));

        assertEquals("La incidencia necesita una categoría", ex.getMessage());
        verify(incidentHistoryService, never()).record(any(), any(), any(), any(), any(), any());
        verify(incidentRepository, never()).save(any());
    }

    @Test
    void resolve_whenChecklistHasPendingItems_throwsConflict_withCountInMessage_andDoesNotSave() {
        Incident incident = incidentWithStatus(IncidentStatus.IN_PROGRESS);
        incident.setCategory(IncidentCategory.PLUMBING);
        when(incidentService.getOwnedByAccountOr404(5L, coordinator)).thenReturn(incident);
        when(incidentChecklistService.countPending(5L)).thenReturn(3L);

        ResolveIncidentRequest request = new ResolveIncidentRequest(30, "Nota");
        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.resolve(5L, request, coordinator));

        assertEquals("Quedan 3 tareas del checklist sin completar", ex.getMessage());
        verify(incidentHistoryService, never()).record(any(), any(), any(), any(), any(), any());
        verify(incidentRepository, never()).save(any());
    }
}