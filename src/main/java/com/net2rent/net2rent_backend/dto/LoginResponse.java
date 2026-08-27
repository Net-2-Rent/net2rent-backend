package com.net2rent.net2rent_backend.dto;

public record LoginResponse(
    String token,
    String email,
    String firstName,
    String role
) {

}
