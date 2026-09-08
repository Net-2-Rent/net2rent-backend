package com.net2rent.net2rent_backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.net2rent.net2rent_backend.dto.request.CreateChecklistItemRequest;
import com.net2rent.net2rent_backend.dto.request.ReorderChecklistRequest;
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

    private IncidentCheckListItem item(Long id, Incident incident, String text, boolean done) {
        return IncidentCheckListItem.builder()
                .id(id).incident(incident).text(text).done(done).position(0).build();
    }

    // ================= addItem =================

    @Test
    void addItem_claimsNewItemAndReturnsCompleteList() {
        Incident incident = Incident.builder().id(5L).status(IncidentStatus.IN_PROGRESS).build();
        CreateChecklistItemRequest request = new CreateChecklistItemRequest("Revisar termostato");

        when(incidentService.getOwnedByAccountOr404(5L, coordinator)).thenReturn(incident);

        // save MUTA el objeto arg asignándole id 99 (como hace Hibernate), porque el
        // servicio addItem NO captura el retorno de save() sino que usa item.getId().
        doAnswer(inv -> {
            IncidentCheckListItem arg = inv.getArgument(0);
            arg.setId(99L);
            return arg;
        }).when(checklistItemRepository).save(any(IncidentCheckListItem.class));

        // La consulta devuelve la lista que ya incluye el nuevo item (id 99)
        when(checklistItemRepository.findByIncident_IdOrderByPositionAscIdAsc(5L))
                .thenReturn(List.of(item(1L, incident, "a", false),
                        item(99L, incident, "Revisar termostato", false),
                        item(2L, incident, "b", true)));
        when(checklistItemRepository.saveAll(anyList()))
                .thenAnswer(inv -> inv.getArgument(0));

        List<ChecklistItemResponse> res = service.addItem(5L, request, coordinator);

        verify(checklistItemRepository).save(any(IncidentCheckListItem.class));
        verify(checklistItemRepository).saveAll(anyList());
        assertEquals(3, res.size());
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

    // ================= setDone =================

    @Test
    void setDone_marksItem_andRecordsWhoAndWhen() {
        Incident incident = Incident.builder().id(5L).status(IncidentStatus.IN_PROGRESS).build();
        IncidentCheckListItem item = item(30L, incident, "Revisar termostato", false);
        AppUser actor = AppUser.builder().id(10L).firstName("Pau").lastName("Roig").build();

        when(incidentService.getOwnedByAccountOr404(5L, coordinator)).thenReturn(incident);
        when(checklistItemRepository.findByIdAndIncident_Id(30L, 5L)).thenReturn(Optional.of(item));
        when(userRepository.getReferenceById(10L)).thenReturn(actor);
        when(checklistItemRepository.save(any(IncidentCheckListItem.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // setDone: 1ª consulta (reconstruir) y persistOrder vuelve a consultar → mismo stub
        when(checklistItemRepository.findByIncident_IdOrderByPositionAscIdAsc(5L))
                .thenReturn(List.of(item));
        when(checklistItemRepository.saveAll(anyList()))
                .thenAnswer(inv -> inv.getArgument(0));

        List<ChecklistItemResponse> res =
                service.setDone(5L, 30L, new UpdateChecklistItemRequest(true), coordinator);

        assertTrue(res.get(0).done());
        assertEquals("Pau Roig", res.get(0).checkedByName());
        assertEquals(LocalDateTime.of(2026, 9, 7, 8, 0), res.get(0).checkedAt());
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

    // ================= deleteItem =================

    @Test
    void deleteItem_removesItem_andReindexes() {
        Incident incident = Incident.builder().id(5L).status(IncidentStatus.IN_PROGRESS).build();
        IncidentCheckListItem item = item(30L, incident, "x", false);

        when(incidentService.getOwnedByAccountOr404(5L, coordinator)).thenReturn(incident);
        when(checklistItemRepository.findByIdAndIncident_Id(30L, 5L)).thenReturn(Optional.of(item));
        // deleteItem: 1ª consulta (tras delete) y persistOrder vuelve a consultar → mismo stub
        when(checklistItemRepository.findByIncident_IdOrderByPositionAscIdAsc(5L))
                .thenReturn(List.of(item(31L, incident, "y", false)));
        when(checklistItemRepository.saveAll(anyList()))
                .thenAnswer(inv -> inv.getArgument(0));

        List<ChecklistItemResponse> res = service.deleteItem(5L, 30L, coordinator);

        verify(checklistItemRepository).delete(item);
        assertEquals(1, res.size());
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

    // ================= list =================

    @Test
    void list_returnsItemsInPositionOrder() {
        Incident incident = Incident.builder().id(5L).status(IncidentStatus.IN_PROGRESS).build();
        IncidentCheckListItem item1 = item(1L, incident, "a", false);
        item1.setPosition(0);
        IncidentCheckListItem item2 = item(2L, incident, "b", true);
        item2.setPosition(1);

        when(incidentService.getOwnedByAccountOr404(5L, coordinator)).thenReturn(incident);
        when(checklistItemRepository.findByIncident_IdOrderByPositionAscIdAsc(5L))
                .thenReturn(List.of(item1, item2));

        List<ChecklistItemResponse> res = service.list(5L, coordinator);

        assertEquals(2, res.size());
        assertEquals("a", res.get(0).text());
        assertEquals(0, res.get(0).position());
        assertEquals("b", res.get(1).text());
        assertEquals(1, res.get(1).position());
    }

    // ================= reorder =================

    @Test
    void reorder_reassignsDensePositionsAndReturnsList() {
        Incident incident = Incident.builder().id(5L).status(IncidentStatus.IN_PROGRESS).build();
        IncidentCheckListItem itemA = item(1L, incident, "a", false);
        IncidentCheckListItem itemB = item(2L, incident, "b", false);
        IncidentCheckListItem itemC = item(3L, incident, "c", true);

        when(incidentService.getOwnedByAccountOr404(5L, coordinator)).thenReturn(incident);
        when(checklistItemRepository.findByIncident_IdOrderByPositionAscIdAsc(5L))
                .thenReturn(List.of(itemA, itemB, itemC));
        when(checklistItemRepository.saveAll(anyList()))
                .thenAnswer(inv -> inv.getArgument(0));

        ReorderChecklistRequest request = new ReorderChecklistRequest(List.of(3L, 1L, 2L));
        List<ChecklistItemResponse> res = service.reorder(5L, request, coordinator);

        assertEquals(3, res.size());
        assertEquals(0, res.get(0).position()); // c → pos 0 (primero en la request)
        assertEquals(1, res.get(1).position()); // a → pos 1
        assertEquals(2, res.get(2).position()); // b → pos 2
    }

    @Test
    void reorder_withWrongIdSet_throwsConflict() {
        Incident incident = Incident.builder().id(5L).status(IncidentStatus.IN_PROGRESS).build();
        IncidentCheckListItem itemA = item(1L, incident, "a", false);
        IncidentCheckListItem itemB = item(2L, incident, "b", false);

        when(incidentService.getOwnedByAccountOr404(5L, coordinator)).thenReturn(incident);
        when(checklistItemRepository.findByIncident_IdOrderByPositionAscIdAsc(5L))
                .thenReturn(List.of(itemA, itemB));

        ReorderChecklistRequest request = new ReorderChecklistRequest(List.of(1L, 99L));

        assertThrows(ConflictException.class, () -> service.reorder(5L, request, coordinator));
        verify(checklistItemRepository, never()).saveAll(any());
    }

    @Test
    void reorder_withDuplicateIds_throwsConflict() {
        Incident incident = Incident.builder().id(5L).status(IncidentStatus.IN_PROGRESS).build();
        IncidentCheckListItem itemA = item(1L, incident, "a", false);
        IncidentCheckListItem itemB = item(2L, incident, "b", false);

        when(incidentService.getOwnedByAccountOr404(5L, coordinator)).thenReturn(incident);
        when(checklistItemRepository.findByIncident_IdOrderByPositionAscIdAsc(5L))
                .thenReturn(List.of(itemA, itemB));

        ReorderChecklistRequest request = new ReorderChecklistRequest(List.of(1L, 1L, 2L));

        assertThrows(ConflictException.class, () -> service.reorder(5L, request, coordinator));
        verify(checklistItemRepository, never()).saveAll(any());
    }

    @Test
    void reorder_onClosedIncident_throws409() {
        Incident closed = Incident.builder().id(5L).status(IncidentStatus.CLOSED).build();
        when(incidentService.getOwnedByAccountOr404(5L, coordinator)).thenReturn(closed);

        assertThrows(ConflictException.class,
                () -> service.reorder(5L, new ReorderChecklistRequest(List.of(1L)), coordinator));
        verify(checklistItemRepository, never()).saveAll(any());
    }
}