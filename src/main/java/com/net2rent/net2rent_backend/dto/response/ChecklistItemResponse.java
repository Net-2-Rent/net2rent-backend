package com.net2rent.net2rent_backend.dto.response;

import com.net2rent.net2rent_backend.model.IncidentCheckListItem;

import java.time.LocalDateTime;

public record ChecklistItemResponse(
        Long id,
        String text,
        boolean done,
        Integer position,
        String checkedByName,
        LocalDateTime checkedAt
) {
    public static ChecklistItemResponse from(IncidentCheckListItem item) {
        String checkedByName = (item.getCheckedBy() == null)
                ? null
                : item.getCheckedBy().getFirstName() + " " + item.getCheckedBy().getLastName();

        return new ChecklistItemResponse(
                item.getId(),
                item.getText(),
                item.isDone(),
                item.getPosition(),
                checkedByName,
                item.getCheckedAt()
        );
    }
}