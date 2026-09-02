package com.net2rent.net2rent_backend.dto.response;

public record LodgingResponse(
        Long id,
        String name,
        String address,
        String ref,
        String accessNotes,
        boolean active) {

}
