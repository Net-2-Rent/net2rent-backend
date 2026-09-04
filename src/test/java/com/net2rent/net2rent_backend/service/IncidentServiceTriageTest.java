package com.net2rent.net2rent_backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.net2rent.net2rent_backend.dto.ClassifyIncidentRequest;
import com.net2rent.net2rent_backend.dto.CorrectIncidentTextRequest;
import com.net2rent.net2rent_backend.exception.NotFoundException;
import com.net2rent.net2rent_backend.model.Account;
import com.net2rent.net2rent_backend.model.AppUser;
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
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// Tests del triage (NET-66): clasificar, marcar urgente y corregir texto.
@ExtendWith(MockitoExtension.class)
class IncidentServiceTriageTest {

    @Mock private IncidentRepository incidentRepository;
    @Mock private IncidentCounterRepository incidentCounterRepository;
    @Mock private IncidentHistoryService incidentHistoryService;
    @Mock private LodgingRepository lodgingRepository;
    @Mock private UserRepository userRepository;

    private IncidentService service;

    private final Clock clock =
            Clock.fixed(Instant.parse("2026-09-02T08:00:00Z"), ZoneOffset.UTC);

    // Usuario del token: coordinador de la cuenta 1.
    private final AuthUser coordinator =
            new AuthUser(10L, 1L, "coord@net2rent.com", "COORDINATOR");

    private Account account;
    private Lodging lodging;

    @BeforeEach
    void setUp() {
        service = new IncidentService(
                incidentRepository, incidentCounterRepository, incidentHistoryService,
                lodgingRepository, userRepository, clock);

        account = Account.builder().id(1L).name("net2Rent Demo").build();
        lodging = Lodging.builder()
                .id(1L).account(account).ref("APT-1001").name("Piso Centro").active(true).build();
    }

    // Incidencia base "del portal": sin categoría, prioridad NORMAL, estado NEW.
    private Incident portalIncident() {
        return Incident.builder()
                .id(100L)
                .account(account)
                .code("INC-2026-000001")
                .status(IncidentStatus.NEW)
                .priority(IncidentPriority.NORMAL)
                .category(null)
                .lodging(lodging)
                .title("Fuga de agua en el baño")
                .description("Fuga de agua en el baño")
                .guestFirstName("Ana").guestLastName("López")
                .openedAt(LocalDateTime.of(2026, 9, 1, 9, 0))
                .createdAt(LocalDateTime.of(2026, 9, 1, 9, 0))
                .build();
    }

    // ---------- CU-INC-04: clasificar ----------

    @Test
    void classify_assignsCategoryAndPriority_recordsBeforeAndAfterInHistory() {
        Incident incident = portalIncident();
        when(incidentRepository.findByIdAndAccount_Id(100L, 1L)).thenReturn(Optional.of(incident));

        service.classify(100L,
                new ClassifyIncidentRequest(IncidentCategory.PLUMBING, IncidentPriority.HIGH),
                coordinator);

        // Los cambios se aplican a la incidencia
        assertEquals(IncidentCategory.PLUMBING, incident.getCategory());
        assertEquals(IncidentPriority.HIGH, incident.getPriority());

        // Se guardan DOS eventos de historial con valor anterior y nuevo
        ArgumentCaptor<IncidentEventType> typeCaptor = ArgumentCaptor.forClass(IncidentEventType.class);
        verify(incidentHistoryService, times(2)).record(
                any(Incident.class), any(), typeCaptor.capture(), any(), any(), any(LocalDateTime.class));
        List<IncidentEventType> types = typeCaptor.getAllValues();
        assertEquals(IncidentEventType.CATEGORY_CHANGED, types.get(0));
        assertEquals(IncidentEventType.PRIORITY_CHANGED, types.get(1));
    }

    @Test
    void classify_whenValuesUnchanged_recordsNoHistory() {
        Incident incident = portalIncident();
        incident.setCategory(IncidentCategory.PLUMBING);
        incident.setPriority(IncidentPriority.HIGH);
        when(incidentRepository.findByIdAndAccount_Id(100L, 1L)).thenReturn(Optional.of(incident));

        service.classify(100L,
                new ClassifyIncidentRequest(IncidentCategory.PLUMBING, IncidentPriority.HIGH),
                coordinator);

        verify(incidentHistoryService, never()).record(any(), any(), any(), any(), any(), any());
    }

    // ---------- CU-INC-05: marcar urgente ----------

    @Test
    void markUrgent_raisesPriority_withoutChangingStatusOrAssignee_recordsHistory() {
        AppUser operator = AppUser.builder()
                .id(2L).account(account).firstName("Marta").lastName("Ruiz")
                .role(UserRole.OPERATOR).active(true).build();

        Incident incident = portalIncident();
        incident.setStatus(IncidentStatus.IN_PROGRESS);
        incident.setPriority(IncidentPriority.NORMAL);
        incident.setAssignee(operator);
        when(incidentRepository.findByIdAndAccount_Id(100L, 1L)).thenReturn(Optional.of(incident));

        service.markUrgent(100L, coordinator);

        assertEquals(IncidentPriority.URGENT, incident.getPriority());
        assertEquals(IncidentStatus.IN_PROGRESS, incident.getStatus());
        assertEquals(operator, incident.getAssignee());

        ArgumentCaptor<IncidentEventType> typeCaptor = ArgumentCaptor.forClass(IncidentEventType.class);
        verify(incidentHistoryService, times(1)).record(
                any(Incident.class), any(), typeCaptor.capture(), any(), any(), any(LocalDateTime.class));
        assertEquals(IncidentEventType.PRIORITY_CHANGED, typeCaptor.getValue());
    }

    // ---------- Corregir título / descripción ----------

    @Test
    void correctText_withoutTitle_generatesTitleFromFirst80CharsOfDescription() {
        Incident incident = portalIncident();
        when(incidentRepository.findByIdAndAccount_Id(100L, 1L)).thenReturn(Optional.of(incident));

        String descripcion =
                "El calentador del baño principal no calienta el agua y hace un ruido muy fuerte por las mañanas";

        service.correctText(100L,
                new CorrectIncidentTextRequest(null, descripcion),
                coordinator);

        assertEquals(descripcion, incident.getDescription());
        assertEquals(80, incident.getTitle().length());
        assertEquals(descripcion.substring(0, 80), incident.getTitle());
    }

    @Test
    void correctText_withCustomTitle_keepsProvidedTitle() {
        Incident incident = portalIncident();
        when(incidentRepository.findByIdAndAccount_Id(100L, 1L)).thenReturn(Optional.of(incident));

        service.correctText(100L,
                new CorrectIncidentTextRequest("Caldera averiada", "La caldera no enciende"),
                coordinator);

        assertEquals("Caldera averiada", incident.getTitle());
        assertEquals("La caldera no enciende", incident.getDescription());
    }

    // ---------- Aislamiento por cuenta ----------

    @Test
    void classify_whenIncidentBelongsToAnotherAccount_throwsNotFound() {
        when(incidentRepository.findByIdAndAccount_Id(999L, 1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                service.classify(999L,
                        new ClassifyIncidentRequest(IncidentCategory.PLUMBING, IncidentPriority.HIGH),
                        coordinator));

        verify(incidentHistoryService, never()).record(any(), any(), any(), any(), any(), any());
    }
}