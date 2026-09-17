package com.net2rent.net2rent_backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.net2rent.net2rent_backend.dto.request.CreateCommentRequest;
import com.net2rent.net2rent_backend.dto.response.TimelineItemResponse;
import com.net2rent.net2rent_backend.exception.ConflictException;
import com.net2rent.net2rent_backend.exception.ForbiddenException;
import com.net2rent.net2rent_backend.exception.NotFoundException;
import com.net2rent.net2rent_backend.model.AppUser;
import com.net2rent.net2rent_backend.model.Incident;
import com.net2rent.net2rent_backend.model.IncidentComment;
import com.net2rent.net2rent_backend.model.enums.IncidentStatus;
import com.net2rent.net2rent_backend.repository.IncidentCommentRepository;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IncidentCommentServiceTest {

    @Mock private IncidentService incidentService;
    @Mock private IncidentAccessPolicy incidentAccessPolicy;
    @Mock private IncidentCommentRepository incidentCommentRepository;
    @Mock private UserRepository userRepository;

    private final Clock clock =
            Clock.fixed(Instant.parse("2026-09-04T08:00:00Z"), ZoneOffset.UTC);

    private IncidentCommentService service;

    private final AuthUser coordinator = new AuthUser(10L, 1L, "coord@net2rent.com", "COORDINATOR");
    private final AuthUser operator    = new AuthUser(20L, 1L, "op@net2rent.com", "OPERATOR");
    private final CreateCommentRequest request = new CreateCommentRequest("Reviso el circuito de gas");

    @BeforeEach
    void setUp() {
        service = new IncidentCommentService(incidentService, incidentAccessPolicy,
                incidentCommentRepository, userRepository, clock);
    }

    @Test
    void addComment_onOpenIncident_savesAndReturnsTimelineItem() {
        Incident incident = Incident.builder().id(5L).status(IncidentStatus.IN_PROGRESS).build();
        AppUser author = AppUser.builder().id(10L).firstName("Pau").lastName("Roig").build();

        when(incidentService.getOwnedByAccountOr404(5L, coordinator)).thenReturn(incident);
        when(userRepository.getReferenceById(10L)).thenReturn(author);
        when(incidentCommentRepository.save(any(IncidentComment.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TimelineItemResponse res = service.addComment(5L, request, coordinator);

        assertEquals("COMMENT", res.type());
        assertEquals("Reviso el circuito de gas", res.text());
        assertEquals("Pau Roig", res.actorName());
        assertEquals(LocalDateTime.of(2026, 9, 4, 8, 0), res.at());

        ArgumentCaptor<IncidentComment> captor = ArgumentCaptor.forClass(IncidentComment.class);
        verify(incidentCommentRepository).save(captor.capture());
        assertSame(incident, captor.getValue().getIncident());
        assertSame(author, captor.getValue().getAuthor());
    }

    @Test
    void addComment_onClosedIncident_throws409_andDoesNotSave() {
        Incident closed = Incident.builder().id(5L).status(IncidentStatus.CLOSED).build();
        when(incidentService.getOwnedByAccountOr404(5L, coordinator)).thenReturn(closed);

        assertThrows(ConflictException.class, () -> service.addComment(5L, request, coordinator));
        verify(incidentCommentRepository, never()).save(any());
    }

    @Test
    void addComment_whenPolicyForbids_throws403_andDoesNotSave() {
        Incident incident = Incident.builder().id(5L).status(IncidentStatus.IN_PROGRESS).build();
        when(incidentService.getOwnedByAccountOr404(5L, operator)).thenReturn(incident);
        doThrow(new ForbiddenException("No puedes editar una incidencia que no tienes asignada"))
                .when(incidentAccessPolicy).ensureCanActOn(incident, operator);

        assertThrows(ForbiddenException.class, () -> service.addComment(5L, request, operator));
        verify(incidentCommentRepository, never()).save(any());
    }

    @Test
    void addComment_whenIncidentNotAccessible_propagates404() {
        when(incidentService.getOwnedByAccountOr404(99L, coordinator))
                .thenThrow(new NotFoundException("Incidencia no encontrada"));

        assertThrows(NotFoundException.class, () -> service.addComment(99L, request, coordinator));
        verifyNoInteractions(incidentAccessPolicy, incidentCommentRepository, userRepository);
    }
}