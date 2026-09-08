package com.net2rent.net2rent_backend.dto;

import java.time.LocalDateTime;
import com.net2rent.net2rent_backend.model.Incident;

public record IncidentResponse(
                Long id,
                String code,
                String status,
                String rejectionReason,
                String priority,
                String category,
                String title,
                String description,
                String guestFirstName,
                String guestLastName,
                String guestContact,
                String lodgingRef,
                String lodgingName,
                String lodgingAddress,
                String lodgingAccessNotes,
                String assigneeName,
                LocalDateTime openedAt
                LocalDateTime openedAt,
                LocalDateTime startedAt,
                String pauseReason) {
        public static IncidentResponse from(Incident i) {
                String assigneeName = (i.getAssignee() == null)
                        ? null
                        : i.getAssignee().getFirstName() + " " + i.getAssignee().getLastName();

                return new IncidentResponse(
                        i.getId(),
                        i.getCode(),
                        i.getStatus() == null ? null : i.getStatus().name(),
                        i.getRejectionReason(),
                        i.getPriority() == null ? null : i.getPriority().name(),
                        i.getCategory() == null ? null : i.getCategory().name(),
                        i.getTitle(),
                        i.getDescription(),
                        i.getGuestFirstName(),
                        i.getGuestLastName(),
                        i.getGuestContact(),
                        i.getLodging() == null ? null : i.getLodging().getRef(),
                        i.getLodging() == null ? null : i.getLodging().getName(),
                        i.getLodging() == null ? null : i.getLodging().getAddress(),
                        i.getLodging() == null ? null : i.getLodging().getAccessNotes(),
                        assigneeName,
                        i.getOpenedAt(),
                        i.getStartedAt(),
                        i.getPauseReason());
        }
}