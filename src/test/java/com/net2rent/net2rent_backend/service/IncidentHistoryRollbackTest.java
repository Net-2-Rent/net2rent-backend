package com.net2rent.net2rent_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.net2rent.net2rent_backend.dto.request.CreateGuestIncidentRequest;
import com.net2rent.net2rent_backend.model.Incident;
import com.net2rent.net2rent_backend.model.enums.IncidentCategory;
import com.net2rent.net2rent_backend.model.enums.IncidentEventType;
import com.net2rent.net2rent_backend.repository.IncidentHistoryRepository;
import com.net2rent.net2rent_backend.repository.IncidentRepository;
import com.net2rent.net2rent_backend.security.GuestPrincipal;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class IncidentHistoryRollbackTest {

    @Autowired private IncidentService incidentService;
    @Autowired private IncidentHistoryService incidentHistoryService;
    @Autowired private IncidentHistoryRepository incidentHistoryRepository;
    @Autowired private IncidentRepository incidentRepository;
    @Autowired private PlatformTransactionManager txManager;

    @Test
    void event_isRolledBack_whenSurroundingTransactionFails() {
        incidentService.registerGuestIncident(
                new CreateGuestIncidentRequest("Ana", "López", null,
                        IncidentCategory.ELECTRICITY, "No hay luz en el salón desde ayer"),
                new GuestPrincipal(1L));

        Incident incident = incidentRepository.findAll().get(0);
        long baseline = incidentHistoryRepository.count();

        TransactionTemplate tx = new TransactionTemplate(txManager);
        assertThrows(RuntimeException.class, () ->
                tx.executeWithoutResult(status -> {
                    incidentHistoryService.record(incident, null,
                            IncidentEventType.STATUS_CHANGED, "NEW", "ASSIGNED",
                            LocalDateTime.now());
                    throw new RuntimeException("fallo simulado tras registrar el evento");
                }));

        assertEquals(baseline, incidentHistoryRepository.count());
    }
}