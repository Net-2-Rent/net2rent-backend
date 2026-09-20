package com.net2rent.net2rent_backend.dto.response;

import java.time.LocalDateTime;

import com.net2rent.net2rent_backend.model.IncidentTimeEntry;

public record TimeEntryResponse(
        Long id,
        String authorName,
        String concept,
        Integer minutes,
        LocalDateTime createdAt) {

    public static TimeEntryResponse from(IncidentTimeEntry entry) {
        String authorName = entry.getAuthor().getFirstName() + " " + entry.getAuthor().getLastName();

        return new TimeEntryResponse(entry.getId(), authorName, entry.getConcept(), entry.getMinutes(), entry.getCreatedAt()
    );
    }

}
