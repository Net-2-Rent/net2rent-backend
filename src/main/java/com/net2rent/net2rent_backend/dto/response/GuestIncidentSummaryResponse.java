package com.net2rent.net2rent_backend.dto.response;

import java.time.LocalDateTime;
import com.net2rent.net2rent_backend.model.Incident;

public record GuestIncidentSummaryResponse(
        Long id,
        String code,
        String description,
        String status,
        LocalDateTime openedAt,
        LocalDateTime closedAt
) {
    public static GuestIncidentSummaryResponse from(Incident i) {
        return new GuestIncidentSummaryResponse(
                i.getId(),
                i.getCode(),
                i.getDescription(),
                i.getStatus() == null ? null : i.getStatus().name(),
                i.getOpenedAt(),
                i.getClosedAt()
        );
    }
}