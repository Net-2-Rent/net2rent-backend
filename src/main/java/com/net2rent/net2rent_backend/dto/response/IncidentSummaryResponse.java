package com.net2rent.net2rent_backend.dto.response;

import com.net2rent.net2rent_backend.model.Incident;
import com.net2rent.net2rent_backend.model.Lodging;

import java.time.LocalDateTime;

public record IncidentSummaryResponse(
        Long id,
        String code,
        String source,
        String lodgingRef,
        String lodgingName,
        String title,
        String category,
        String priority,
        String status,
        String assigneeName,
        LocalDateTime openedAt
) {
    public static IncidentSummaryResponse from(Incident i) {
        String assigneeName = (i.getAssignee() == null)
                ? null
                : i.getAssignee().getFirstName() + " " + i.getAssignee().getLastName();

        Lodging lodging = i.getLodging();

        return new IncidentSummaryResponse(
                i.getId(),
                i.getCode(),
                i.getSource()   == null ? null : i.getSource().name(),
                lodging == null ? null : lodging.getRef(),
                lodging == null ? null : lodging.getName(),
                i.getTitle(),
                i.getCategory() == null ? null : i.getCategory().name(),
                i.getPriority() == null ? null : i.getPriority().name(),
                i.getStatus()   == null ? null : i.getStatus().name(),
                assigneeName,
                i.getOpenedAt()
        );
    }
}