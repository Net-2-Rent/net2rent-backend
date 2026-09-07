package com.net2rent.net2rent_backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.net2rent.net2rent_backend.dto.request.CreateChecklistItemRequest;
import com.net2rent.net2rent_backend.dto.request.UpdateChecklistItemRequest;
import com.net2rent.net2rent_backend.dto.response.ChecklistItemResponse;
import com.net2rent.net2rent_backend.exception.ConflictException;
import com.net2rent.net2rent_backend.exception.ForbiddenException;
import com.net2rent.net2rent_backend.exception.NotFoundException;
import com.net2rent.net2rent_backend.model.AppUser;
import com.net2rent.net2rent_backend.model.Incident;
import com.net2rent.net2rent_backend.model.IncidentCheckListItem;
import com.net2rent.net2rent_backend.model.enums.IncidentStatus;
import com.net2rent.net2rent_backend.repository.IncidentCheckListItemRepository;
import com.net2rent.net2rent_backend.repository.UserRepository;
import com.net2rent.net2rent_backend.security.AuthUser;
import com.net2rent.net2rent_backend.security.IncidentAccessPolicy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IncidentChecklistServiceTest {

    @Mock private IncidentService incidentService;
    @Mock private IncidentAccessPolicy incidentAccessPolicy;
    @Mock private IncidentCheckListItemRepository checklistItemRepository;
    @Mock private UserRepository userRepository;

    private final Clock clock =
            Clock.fixed(Instant.parse("2026-09-07T08:00:00Z"), ZoneOffset.UTC);

    private IncidentChecklistService service;

    private final AuthUser coordinator = new AuthUser(10L, 1L, "coord@net2rent.com", "COORDINATOR");
    private final AuthUser operator    = new AuthUser(20L, 1L, "op@net2rent.com", "OPERATOR");

    @BeforeEach
    void setUp() {
        service = new IncidentChecklistService(incidentService, incidentAccessPolicy,
                checklistItemRepository, userRepository, clock);
    }

    // ---------- addItem (CU-CHK-01/02) ----------

    @Test
    void addItem_onOpenIncident_savesAndReturnsItem() {
        Incident incident = Incident.builder().id(5L).status(IncidentStatus.IN_PROGRESS).build();
        CreateChecklistItemRequest request = new CreateChecklistItemRequest("Revisar termostato");

        when(incidentService.getOwnedByAccountOr404(5L, coordinator)).thenReturn(incident);
        when(checklistItemRepository.save(any(IncidentCheckListItem.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ChecklistItemResponse res = service.addItem(5L, request, coordinator);

        assertEquals("Revisar termostato", res.text());
        assertFalse(res.done());

        ArgumentCaptor<IncidentCheckListItem> captor = ArgumentCaptor.forClass(IncidentCheckListItem.class);
        verify(checklistItemRepository).save(captor.capture());
        assertSame(incident, captor.getValue().getIncident());
        assertFalse(captor.getValue().isDone());
    }

    @Test
    void addItem_onClosedIncident_throws409_andDoesNotSave() {
        Incident closed = Incident.builder().id(5L).status(IncidentStatus.CLOSED).build();
        when(incidentService.getOwnedByAccountOr404(5L, coordinator)).thenReturn(closed);

        assertThrows(ConflictException.class,
                () -> service.addItem(5L, new CreateChecklistItemRequest("x"), coordinator));
        verify(checklistItemRepository, never()).save(any());
    }

    @Test
    void addItem_whenPolicyForbids_throws403_andDoesNotSave() {
        Incident incident = Incident.builder().id(5L).status(IncidentStatus.IN_PROGRESS).build();
        when(incidentService.getOwnedByAccountOr404(5L, operator)).thenReturn(incident);
        doThrow(new ForbiddenException("No puedes editar una incidencia que no tienes asignada"))
                .when(incidentAccessPolicy).ensureCanActOn(incident, operator);

        assertThrows(ForbiddenException.class,
                () -> service.addItem(5L, new CreateChecklistItemRequest("x"), operator));
        verify(checklistItemRepository, never()).save(any());
    }

    @Test
    void addItem_whenIncidentNotAccessible_propagates404() {
        when(incidentService.getOwnedByAccountOr404(99L, coordinator))
                .thenThrow(new NotFoundException("Incidencia no encontrada"));

        assertThrows(NotFoundException.class,
                () -> service.addItem(99L, new CreateChecklistItemRequest("x"), coordinator));
        verifyNoInteractions(incidentAccessPolicy, checklistItemRepository);
    }

    // ---------- setDone (CU-CHK-03/04) ----------

    @Test
    void setDone_marksItem_andRecordsWhoAndWhen() {
        Incident incident = Incident.builder().id(5L).status(IncidentStatus.IN_PROGRESS).build();
        IncidentCheckListItem item = IncidentCheckListItem.builder()
                .id(30L).incident(incident).text("Revisar termostato").done(false).build();
        AppUser actor = AppUser.builder().id(10L).firstName("Pau").lastName("Roig").build();

        when(incidentService.getOwnedByAccountOr404(5L, coordinator)).thenReturn(incident);
        when(checklistItemRepository.findByIdAndIncident_Id(30L, 5L)).thenReturn(Optional.of(item));
        when(userRepository.getReferenceById(10L)).thenReturn(actor);
        when(checklistItemRepository.save(any(IncidentCheckListItem.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ChecklistItemResponse res = service.setDone(5L, 30L, new UpdateChecklistItemRequest(true), coordinator);

        assertTrue(res.done());
        assertEquals("Pau Roig", res.checkedByName());
        assertEquals(LocalDateTime.of(2026, 9, 7, 8, 0), res.checkedAt());
    }

    @Test
    void setDone_onClosedIncident_throws409() {
        Incident closed = Incident.builder().id(5L).status(IncidentStatus.CLOSED).build();
        when(incidentService.getOwnedByAccountOr404(5L, coordinator)).thenReturn(closed);

        assertThrows(ConflictException.class,
                () -> service.setDone(5L, 30L, new UpdateChecklistItemRequest(true), coordinator));
        verify(checklistItemRepository, never()).save(any());
    }

    @Test
    void setDone_whenItemBelongsToAnotherIncident_throws404() {
        Incident incident = Incident.builder().id(5L).status(IncidentStatus.IN_PROGRESS).build();
        when(incidentService.getOwnedByAccountOr404(5L, coordinator)).thenReturn(incident);
        when(checklistItemRepository.findByIdAndIncident_Id(30L, 5L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.setDone(5L, 30L, new UpdateChecklistItemRequest(true), coordinator));
        verify(checklistItemRepository, never()).save(any());
    }

    // ---------- deleteItem (CU-CHK-05) ----------

    @Test
    void deleteItem_removesItem() {
        Incident incident = Incident.builder().id(5L).status(IncidentStatus.IN_PROGRESS).build();
        IncidentCheckListItem item = IncidentCheckListItem.builder().id(30L).incident(incident).build();

        when(incidentService.getOwnedByAccountOr404(5L, coordinator)).thenReturn(incident);
        when(checklistItemRepository.findByIdAndIncident_Id(30L, 5L)).thenReturn(Optional.of(item));

        service.deleteItem(5L, 30L, coordinator);

        verify(checklistItemRepository).delete(item);
    }

    @Test
    void deleteItem_onRejectedIncident_throws409_andDoesNotDelete() {
        Incident rejected = Incident.builder().id(5L).status(IncidentStatus.REJECTED).build();
        when(incidentService.getOwnedByAccountOr404(5L, coordinator)).thenReturn(rejected);

        assertThrows(ConflictException.class, () -> service.deleteItem(5L, 30L, coordinator));
        verify(checklistItemRepository, never()).delete(any());
    }

    @Test
    void deleteItem_whenPolicyForbids_throws403_andDoesNotDelete() {
        Incident incident = Incident.builder().id(5L).status(IncidentStatus.IN_PROGRESS).build();
        when(incidentService.getOwnedByAccountOr404(5L, operator)).thenReturn(incident);
        doThrow(new ForbiddenException("No puedes editar una incidencia que no tienes asignada"))
                .when(incidentAccessPolicy).ensureCanActOn(incident, operator);

        assertThrows(ForbiddenException.class, () -> service.deleteItem(5L, 30L, operator));
        verify(checklistItemRepository, never()).delete(any());
    }

    // ---------- list ----------

    @Test
    void list_returnsItemsInRepositoryOrder() {
        Incident incident = Incident.builder().id(5L).status(IncidentStatus.IN_PROGRESS).build();
        IncidentCheckListItem item1 = IncidentCheckListItem.builder().id(1L).incident(incident).text("a").build();
        IncidentCheckListItem item2 = IncidentCheckListItem.builder().id(2L).incident(incident).text("b").build();

        when(incidentService.getOwnedByAccountOr404(5L, coordinator)).thenReturn(incident);
        when(checklistItemRepository.findByIncident_IdOrderByIdAsc(5L)).thenReturn(List.of(item1, item2));

        List<ChecklistItemResponse> res = service.list(5L, coordinator);

        assertEquals(2, res.size());
        assertEquals("a", res.get(0).text());
        assertEquals("b", res.get(1).text());
    }
}