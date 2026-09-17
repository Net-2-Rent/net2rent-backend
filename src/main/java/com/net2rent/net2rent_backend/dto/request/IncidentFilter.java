package com.net2rent.net2rent_backend.dto.request;

import com.net2rent.net2rent_backend.model.enums.IncidentCategory;
import com.net2rent.net2rent_backend.model.enums.IncidentPriority;
import com.net2rent.net2rent_backend.model.enums.IncidentStatus;

import java.time.LocalDate;

public record IncidentFilter (
        IncidentStatus status,
        IncidentPriority priority,
        IncidentCategory category,
        Long lodgingId,
        Long assigneeId,
        Boolean unassigned,
        LocalDate openedFrom,
        LocalDate openedTo
) {
}