package com.net2rent.net2rent_backend.dto.response;

import java.time.LocalDateTime;
import com.net2rent.net2rent_backend.model.Incident;

public record GuestIncidentDetailResponse(
        Long id,
        String code,
        String description,
        String status,
        LocalDateTime openedAt,
        LocalDateTime closedAt
) {
    public static GuestIncidentDetailResponse from(Incident i) {
        return new GuestIncidentDetailResponse(
                i.getId(),
                i.getCode(),
                i.getDescription(),
                i.getStatus() == null ? null : i.getStatus().name(),
                i.getOpenedAt(),
                i.getClosedAt()
        );
    }
}