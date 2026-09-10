package com.net2rent.net2rent_backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.net2rent.net2rent_backend.dto.ClassifyIncidentRequest;
import com.net2rent.net2rent_backend.dto.CorrectIncidentTextRequest;
import com.net2rent.net2rent_backend.exception.ConflictException;
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

// CU-INC-13: editar una incidencia en estado terminal (CLOSED/REJECTED) da 409.
@ExtendWith(MockitoExtension.class)
class IncidentServiceTerminalGuardTest {

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

    // Incidencia base en el estado terminal que se le pase, de la cuenta 1.
    private Incident incidentWithStatus(IncidentStatus status) {
        return Incident.builder()
                .id(100L)
                .account(account)
                .code("INC-2026-000001")
                .status(status)
                .priority(IncidentPriority.NORMAL)
                .category(IncidentCategory.PLUMBING)
                .lodging(lodging)
                .title("Fuga de agua")
                .description("Fuga de agua")
                .openedAt(LocalDateTime.of(2026, 9, 1, 9, 0))
                .createdAt(LocalDateTime.of(2026, 9, 1, 9, 0))
                .build();
    }

    @Test
    void classify_whenClosed_throwsConflict() {
        Incident incident = incidentWithStatus(IncidentStatus.CLOSED);
        when(incidentRepository.findByIdAndAccount_Id(100L, 1L)).thenReturn(Optional.of(incident));

        assertThrows(ConflictException.class, () ->
                service.classify(100L,
                        new ClassifyIncidentRequest(IncidentCategory.HVAC, IncidentPriority.HIGH),
                        coordinator));

        // No cambia nada ni deja rastro
        verify(incidentHistoryService, never()).record(any(), any(), any(), any(), any(), any(), any());
        verify(incidentRepository, never()).save(any());
    }

    @Test
    void correctText_whenRejected_throwsConflict() {
        Incident incident = incidentWithStatus(IncidentStatus.REJECTED);
        when(incidentRepository.findByIdAndAccount_Id(100L, 1L)).thenReturn(Optional.of(incident));

        assertThrows(ConflictException.class, () ->
                service.correctText(100L,
                        new CorrectIncidentTextRequest("Nuevo título", "Nueva descripción"),
                        coordinator));

        verify(incidentHistoryService, never()).record(any(), any(), any(), any(), any(), any(), any());
        verify(incidentRepository, never()).save(any());
    }

    @Test
    void markUrgent_whenClosed_throwsConflict() {
        Incident incident = incidentWithStatus(IncidentStatus.CLOSED);
        when(incidentRepository.findByIdAndAccount_Id(100L, 1L)).thenReturn(Optional.of(incident));

        assertThrows(ConflictException.class, () ->
                service.markUrgent(100L, coordinator));

        verify(incidentHistoryService, never()).record(any(), any(), any(), any(), any(), any(), any());
        verify(incidentRepository, never()).save(any());
    }
}