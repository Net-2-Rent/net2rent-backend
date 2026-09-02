package com.net2rent.net2rent_backend.dto.response;

import java.time.LocalDateTime;
import com.net2rent.net2rent_backend.model.Incident;

public record GuestIncidentSummaryResponse(
        String code,
        String description,
        String status,
        LocalDateTime openedAt
) {
    public static GuestIncidentSummaryResponse from(Incident i) {
        return new GuestIncidentSummaryResponse(
                i.getCode(),
                i.getDescription(),
                i.getStatus() == null ? null : i.getStatus().name(),
                i.getOpenedAt()
        );
    }
}