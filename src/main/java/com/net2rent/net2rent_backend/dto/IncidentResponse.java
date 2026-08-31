package com.net2rent.net2rent_backend.dto;

import java.time.LocalDateTime;
import com.net2rent.net2rent_backend.model.Incident;

public record IncidentResponse(
        Long id,
        String code,
        String status,
        String priority,
        String category,
        String title,
        String lodgingRef,
        String assigneeName,
        LocalDateTime openedAt
) {
    public static IncidentResponse from(Incident i) {
        String assigneeName = (i.getAssignee() == null)
                ? null
                : i.getAssignee().getFirstName() + " " + i.getAssignee().getLastName();

        return new IncidentResponse(
                i.getId(),
                i.getCode(),
                i.getStatus()   == null ? null : i.getStatus().name(),
                i.getPriority() == null ? null : i.getPriority().name(),
                i.getCategory() == null ? null : i.getCategory().name(),
                i.getTitle(),
                i.getLodging()  == null ? null : i.getLodging().getRef(),
                assigneeName,
                i.getOpenedAt()
        );
    }
}