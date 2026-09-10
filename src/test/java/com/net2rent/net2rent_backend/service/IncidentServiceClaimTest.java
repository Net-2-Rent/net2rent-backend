package com.net2rent.net2rent_backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.net2rent.net2rent_backend.exception.ConflictException;
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
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IncidentServiceClaimTest {

    @Mock private IncidentRepository incidentRepository;
    @Mock private IncidentCounterRepository incidentCounterRepository;
    @Mock private IncidentHistoryService incidentHistoryService;
    @Mock private LodgingRepository lodgingRepository;
    @Mock private UserRepository userRepository;
    @Mock private IncidentImageService incidentImageService;

    private IncidentService service;

    private final Clock clock =
            Clock.fixed(Instant.parse("2026-09-08T08:00:00Z"), ZoneOffset.UTC);

    private final AuthUser operator =
            new AuthUser(20L, 1L, "operario@net2rent.com", "OPERATOR");

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

    private Incident poolIncident() {
        return Incident.builder()
                .id(100L)
                .account(account)
                .code("INC-2026-000001")
                .status(IncidentStatus.NEW)
                .priority(IncidentPriority.NORMAL)
                .lodging(lodging)
                .title("Fuga de agua en el baño")
                .description("Fuga de agua en el baño")
                .guestFirstName("Ana").guestLastName("López")
                .openedAt(LocalDateTime.of(2026, 9, 1, 9, 0))
                .createdAt(LocalDateTime.of(2026, 9, 1, 9, 0))
                .build();
    }

    @Test
    void claim_whenUnassignedNew_assignsOperator_setsAssignedAt_recordsHistory() {
        Incident incident = poolIncident();
        AppUser operatorEntity = AppUser.builder().id(20L).role(UserRole.OPERATOR).build();

        when(incidentRepository.findByIdAndAccount_IdForUpdate(100L, 1L))
                .thenReturn(Optional.of(incident));
        when(userRepository.getReferenceById(20L)).thenReturn(operatorEntity);

        service.claim(100L, operator);

        assertEquals(IncidentStatus.ASSIGNED, incident.getStatus());
        assertSame(operatorEntity, incident.getAssignee());
        assertNotNull(incident.getAssignedAt());

        verify(incidentHistoryService).record(
                any(Incident.class), eq(operatorEntity), eq(IncidentEventType.ASSIGNED),
                isNull(), eq("20"), isNull(), any(LocalDateTime.class));
        verify(incidentRepository).save(incident);
    }

    @Test
    void claim_whenAlreadyAssigned_throwsConflict_withBacklogMessage() {
        Incident incident = poolIncident();
        incident.setStatus(IncidentStatus.ASSIGNED);
        incident.setAssignee(AppUser.builder().id(99L).build()); // ya tiene operario

        when(incidentRepository.findByIdAndAccount_IdForUpdate(100L, 1L))
                .thenReturn(Optional.of(incident));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.claim(100L, operator));
        assertEquals("Esta incidencia ya ha sido asignada", ex.getMessage());

        verify(incidentHistoryService, never()).record(any(), any(), any(), any(), any(), any(), any());
        verify(incidentRepository, never()).save(any());
    }

    @Test
    void claim_whenUnassignedButNotNew_throwsConflict() {
        Incident incident = poolIncident();
        incident.setStatus(IncidentStatus.IN_PROGRESS);

        when(incidentRepository.findByIdAndAccount_IdForUpdate(100L, 1L))
                .thenReturn(Optional.of(incident));

        assertThrows(ConflictException.class, () -> service.claim(100L, operator));

        verify(incidentHistoryService, never()).record(any(), any(), any(), any(), any(), any(), any());
        verify(incidentRepository, never()).save(any());
    }

    @Test
    void claim_whenIncidentBelongsToAnotherAccount_throwsNotFound() {
        when(incidentRepository.findByIdAndAccount_IdForUpdate(999L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.claim(999L, operator));

        verify(incidentHistoryService, never()).record(any(), any(), any(), any(), any(), any(), any());
        verify(incidentRepository, never()).save(any());
    }
}