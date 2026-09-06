package com.net2rent.net2rent_backend.dto.response;

import com.net2rent.net2rent_backend.model.AppUser;
import com.net2rent.net2rent_backend.model.IncidentComment;
import com.net2rent.net2rent_backend.model.IncidentHistory;

import java.time.LocalDateTime;

public record TimelineItemResponse(
        String type,
        LocalDateTime at,
        String actorName,
        String eventType,
        String previousValue,
        String newValue,
        String text
) {
    public static TimelineItemResponse fromEvent(IncidentHistory h) {
        return new TimelineItemResponse(
                "EVENT",
                h.getCreatedAt(),
                actorName(h.getActor()),
                h.getEventType(),
                h.getPreviousValue(),
                h.getNewValue(),
                null);
    }

    public static TimelineItemResponse fromComment(IncidentComment c) {
        return new TimelineItemResponse(
                "COMMENT",
                c.getCreatedAt(),
                actorName(c.getAuthor()),
                null, null, null,
                c.getText());
    }

    private static String actorName(AppUser u) {
        return u == null ? null : u.getFirstName() + " " + u.getLastName();
    }
}