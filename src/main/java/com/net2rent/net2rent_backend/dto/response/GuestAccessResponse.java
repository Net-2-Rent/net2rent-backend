package com.net2rent.net2rent_backend.dto.response;

public record GuestAccessResponse(
    String token,
    Long lodgingId,
    String lodgingName,
    long expiresInSeconds
) {

}
