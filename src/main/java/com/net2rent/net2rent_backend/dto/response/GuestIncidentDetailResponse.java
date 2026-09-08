package com.net2rent.net2rent_backend.dto.response;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import com.net2rent.net2rent_backend.model.Incident;
import com.net2rent.net2rent_backend.model.IncidentImage;

public record GuestIncidentDetailResponse(
        Long id,
        String code,
        String description,
        String status,
        String rejectionReason,
        LocalDateTime openedAt,
        LocalDateTime resolvedAt,
        LocalDateTime closedAt,
        List<String> images
) {
    public static GuestIncidentDetailResponse from(Incident i) {
        return new GuestIncidentDetailResponse(
                i.getId(),
                i.getCode(),
                i.getDescription(),
                i.getStatus() == null ? null : i.getStatus().name(),
                i.getRejectionReason(),
                i.getOpenedAt(),
                i.getResolvedAt(),
                i.getClosedAt(),
                i.getImages().stream().map(GuestIncidentDetailResponse::toDataUri).toList()
        );
    }

    private static String toDataUri(IncidentImage img) {
        return "data:" + img.getContentType() + ";base64," + Base64.getEncoder().encodeToString(img.getData());
    }
}