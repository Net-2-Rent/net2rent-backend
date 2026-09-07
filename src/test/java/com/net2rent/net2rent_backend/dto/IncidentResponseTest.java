package com.net2rent.net2rent_backend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.net2rent.net2rent_backend.model.Account;
import com.net2rent.net2rent_backend.model.Incident;
import com.net2rent.net2rent_backend.model.Lodging;
import com.net2rent.net2rent_backend.model.enums.IncidentPriority;
import com.net2rent.net2rent_backend.model.enums.IncidentSource;
import com.net2rent.net2rent_backend.model.enums.IncidentStatus;

class IncidentResponseTest {

     private Account account() {
        return Account.builder().id(1L).name("net2Rent Demo").build();
    }

    @Test
    void from_includesLodgingAccessNotes_forOperatorFacingResponse() {
        Lodging lodging = Lodging.builder()
                .id(1L)
                .account(account())
                .ref("APT-1001")
                .name("Piso Centro")
                .accessNotes("Llave bajo el felpudo. Código del portal: 4477.")
                .active(true)
                .build();

        Incident incident = Incident.builder()
                .id(1L)
                .account(account())
                .code("INC-2026-000001")
                .source(IncidentSource.PHONE)
                .status(IncidentStatus.NEW)
                .priority(IncidentPriority.NORMAL)
                .lodging(lodging)
                .title("Aire acondicionado no enfría")
                .description("El aire acondicionado del salón no enfría.")
                .guestFirstName("Ana")
                .guestLastName("López")
                .openedAt(LocalDateTime.of(2026, 9, 1, 9, 0))
                .createdAt(LocalDateTime.of(2026, 9, 1, 9, 0))
                .build();

        IncidentResponse response = IncidentResponse.from(incident);

        assertEquals("APT-1001", response.lodgingRef());
        assertEquals("Llave bajo el felpudo. Código del portal: 4477.", response.lodgingAccessNotes());
        assertEquals("Ana", response.guestFirstName());
        assertEquals("López", response.guestLastName());
    }

    @Test
    void from_withoutLodging_leavesAccessNotesNull() {
        Incident incident = Incident.builder()
                .id(1L)
                .account(account())
                .code("INC-2026-000002")
                .source(IncidentSource.PHONE)
                .status(IncidentStatus.NEW)
                .priority(IncidentPriority.NORMAL)
                .lodging(null)
                .title("Incidencia sin alojamiento")
                .description("Caso límite: sin alojamiento asociado.")
                .guestFirstName("Ana")
                .guestLastName("López")
                .openedAt(LocalDateTime.of(2026, 9, 1, 9, 0))
                .createdAt(LocalDateTime.of(2026, 9, 1, 9, 0))
                .build();

        IncidentResponse response = IncidentResponse.from(incident);

        assertNull(response.lodgingRef());
        assertNull(response.lodgingAccessNotes());
    }

}
