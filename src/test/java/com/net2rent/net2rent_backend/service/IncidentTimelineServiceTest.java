package com.net2rent.net2rent_backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.net2rent.net2rent_backend.dto.response.TimelineItemResponse;
import com.net2rent.net2rent_backend.exception.NotFoundException;
import com.net2rent.net2rent_backend.model.AppUser;
import com.net2rent.net2rent_backend.model.IncidentComment;
import com.net2rent.net2rent_backend.model.IncidentHistory;
import com.net2rent.net2rent_backend.repository.IncidentCommentRepository;
import com.net2rent.net2rent_backend.repository.IncidentHistoryRepository;
import com.net2rent.net2rent_backend.security.AuthUser;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IncidentTimelineServiceTest {

    @Mock private IncidentService incidentService;
    @Mock private IncidentHistoryRepository incidentHistoryRepository;
    @Mock private IncidentCommentRepository incidentCommentRepository;
    @InjectMocks private IncidentTimelineService service;

    private final AuthUser coordinator =
            new AuthUser(10L, 1L, "coord@net2rent.com", "COORDINATOR");
    private final Long incidentId = 5L;

    private final AppUser actor =
            AppUser.builder().id(10L).firstName("Pau").lastName("Roig").build();

    @Test
    void getTimeline_mergesEventsAndComments_inChronologicalOrder() {
        IncidentHistory created = IncidentHistory.builder()
                .actor(actor).eventType("CREATED").previousValue(null).newValue("NEW")
                .createdAt(LocalDateTime.of(2026, 9, 4, 10, 0)).build();
        IncidentHistory priority = IncidentHistory.builder()
                .actor(actor).eventType("PRIORITY_CHANGED").previousValue("NORMAL").newValue("URGENT")
                .createdAt(LocalDateTime.of(2026, 9, 4, 10, 30)).build();
        IncidentComment comment = IncidentComment.builder()
                .author(actor).text("Reviso el circuito de gas")
                .createdAt(LocalDateTime.of(2026, 9, 4, 10, 15)).build();

        when(incidentHistoryRepository.findByIncident_IdOrderByCreatedAtAsc(incidentId))
                .thenReturn(List.of(created, priority));
        when(incidentCommentRepository.findByIncident_IdOrderByCreatedAtAsc(incidentId))
                .thenReturn(List.of(comment));

        List<TimelineItemResponse> timeline = service.getTimeline(incidentId, coordinator);

        assertEquals(3, timeline.size());
        assertEquals("EVENT", timeline.get(0).type());
        assertEquals("CREATED", timeline.get(0).eventType());
        assertEquals("Pau Roig", timeline.get(0).actorName());
        assertEquals("COMMENT", timeline.get(1).type());
        assertEquals("Reviso el circuito de gas", timeline.get(1).text());
        assertEquals("EVENT", timeline.get(2).type());
        assertEquals("PRIORITY_CHANGED", timeline.get(2).eventType());
    }

    @Test
    void getTimeline_onEqualTimestamps_keepsEventBeforeComment() {
        LocalDateTime sameInstant = LocalDateTime.of(2026, 9, 4, 10, 0);
        IncidentHistory event = IncidentHistory.builder()
                .actor(actor).eventType("ASSIGNED").newValue("ASSIGNED")
                .createdAt(sameInstant).build();
        IncidentComment comment = IncidentComment.builder()
                .author(actor).text("comentario del mismo segundo")
                .createdAt(sameInstant).build();

        when(incidentHistoryRepository.findByIncident_IdOrderByCreatedAtAsc(incidentId))
                .thenReturn(List.of(event));
        when(incidentCommentRepository.findByIncident_IdOrderByCreatedAtAsc(incidentId))
                .thenReturn(List.of(comment));

        List<TimelineItemResponse> timeline = service.getTimeline(incidentId, coordinator);

        assertEquals("EVENT", timeline.get(0).type());
        assertEquals("COMMENT", timeline.get(1).type());
    }

    @Test
    void getTimeline_systemEvent_hasNullActorName() {
        IncidentHistory systemEvent = IncidentHistory.builder()
                .actor(null).eventType("CREATED").newValue("NEW")
                .createdAt(LocalDateTime.of(2026, 9, 4, 10, 0)).build();

        when(incidentHistoryRepository.findByIncident_IdOrderByCreatedAtAsc(incidentId))
                .thenReturn(List.of(systemEvent));
        when(incidentCommentRepository.findByIncident_IdOrderByCreatedAtAsc(incidentId))
                .thenReturn(List.of());

        List<TimelineItemResponse> timeline = service.getTimeline(incidentId, coordinator);

        assertNull(timeline.get(0).actorName());
    }

    @Test
    void getTimeline_whenIncidentNotAccessible_propagates404_andSkipsRepos() {
        when(incidentService.getOwnedByAccountOr404(incidentId, coordinator))
                .thenThrow(new NotFoundException("Incidencia no encontrada"));

        assertThrows(NotFoundException.class,
                () -> service.getTimeline(incidentId, coordinator));

        verifyNoInteractions(incidentHistoryRepository, incidentCommentRepository);
    }
}