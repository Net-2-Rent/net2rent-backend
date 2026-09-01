package com.net2rent.net2rent_backend.dto;

import com.net2rent.net2rent_backend.model.Lodging;

public record LodgingResponse(
        Long id,
        String ref,
        String name,
        String address,
        String accessNotes,
        boolean active
) {
    public static LodgingResponse from(Lodging lodging) {
        return new LodgingResponse(
                lodging.getId(),
                lodging.getRef(),
                lodging.getName(),
                lodging.getAddress(),
                lodging.getAccessNotes(),
                lodging.isActive()
        );
    }
}