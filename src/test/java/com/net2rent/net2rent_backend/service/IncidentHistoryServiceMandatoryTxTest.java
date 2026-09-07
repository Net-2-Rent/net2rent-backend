package com.net2rent.net2rent_backend.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.net2rent.net2rent_backend.model.Incident;
import com.net2rent.net2rent_backend.model.enums.IncidentEventType;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;

@SpringBootTest
@ActiveProfiles("test")
class IncidentHistoryServiceMandatoryTxTest {

    @Autowired private IncidentHistoryService incidentHistoryService;

    @Test
    void record_withoutOpenTransaction_isRejected() {
        Incident incident = Incident.builder().id(1L).build();

        assertThrows(IllegalTransactionStateException.class, () ->
                incidentHistoryService.record(incident, null, IncidentEventType.CREATED,
                        null, "NEW", LocalDateTime.now()));
    }
}