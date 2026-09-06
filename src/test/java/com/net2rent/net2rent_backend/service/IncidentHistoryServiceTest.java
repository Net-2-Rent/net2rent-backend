package com.net2rent.net2rent_backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

import com.net2rent.net2rent_backend.model.AppUser;
import com.net2rent.net2rent_backend.model.Incident;
import com.net2rent.net2rent_backend.model.IncidentHistory;
import com.net2rent.net2rent_backend.model.enums.IncidentEventType;
import com.net2rent.net2rent_backend.repository.IncidentHistoryRepository;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IncidentHistoryServiceTest {

    @Mock private IncidentHistoryRepository incidentHistoryRepository;
    @InjectMocks private IncidentHistoryService service;

    @Test
    void record_buildsEntryWithAllFields_andSaves() {
        Incident incident = Incident.builder().id(5L).build();
        AppUser actor = AppUser.builder().id(10L).build();
        LocalDateTime occurredAt = LocalDateTime.of(2026, 9, 4, 8, 0);

        service.record(incident, actor, IncidentEventType.PRIORITY_CHANGED,
                "NORMAL", "URGENT", occurredAt);

        ArgumentCaptor<IncidentHistory> captor = ArgumentCaptor.forClass(IncidentHistory.class);
        verify(incidentHistoryRepository).save(captor.capture());

        IncidentHistory saved = captor.getValue();
        assertSame(incident, saved.getIncident());
        assertSame(actor, saved.getActor());
        assertEquals("PRIORITY_CHANGED", saved.getEventType());
        assertEquals("NORMAL", saved.getPreviousValue());
        assertEquals("URGENT", saved.getNewValue());
        assertEquals(occurredAt, saved.getCreatedAt());
    }

    @Test
    void record_allowsNullActor_forSystemEvents() {
        Incident incident = Incident.builder().id(5L).build();
        LocalDateTime occurredAt = LocalDateTime.of(2026, 9, 4, 8, 0);

        service.record(incident, null, IncidentEventType.CREATED, null, "NEW", occurredAt);

        ArgumentCaptor<IncidentHistory> captor = ArgumentCaptor.forClass(IncidentHistory.class);
        verify(incidentHistoryRepository).save(captor.capture());

        IncidentHistory saved = captor.getValue();
        assertNull(saved.getActor());
        assertEquals("CREATED", saved.getEventType());
        assertNull(saved.getPreviousValue());
        assertEquals("NEW", saved.getNewValue());
    }
}