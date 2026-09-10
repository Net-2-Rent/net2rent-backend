package com.net2rent.net2rent_backend.dto.response;

import com.net2rent.net2rent_backend.model.AppUser;
import com.net2rent.net2rent_backend.model.IncidentComment;
import com.net2rent.net2rent_backend.model.IncidentHistory;

import java.time.LocalDateTime;
import java.util.Map;

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
        return fromEvent(h, Map.of());
    }

    public static TimelineItemResponse fromEvent(IncidentHistory h, Map<Long, String> operatorNames) {
        String eventType = h.getEventType();
        String prev = h.getPreviousValue();
        String next = h.getNewValue();
        if (referencesOperator(eventType)) {
            prev = displayName(prev, operatorNames);
            next = displayName(next, operatorNames);
        }
        return new TimelineItemResponse(
                "EVENT", h.getCreatedAt(), actorName(h.getActor()),
                eventType, prev, next, h.getNote());
    }

    public static boolean referencesOperator(String eventType) {
        return "ASSIGNED".equals(eventType)
                || "REASSIGNED".equals(eventType)
                || "UNASSIGNED".equals(eventType);
    }

    private static String displayName(String value, Map<Long, String> operatorNames) {
        if (value == null) return null;
        try {
            return operatorNames.getOrDefault(Long.valueOf(value.trim()), value);
        } catch (NumberFormatException e) {
            return value;
        }
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