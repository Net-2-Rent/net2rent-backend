package com.net2rent.net2rent_backend.security;

public record AuthUser(Long userId, Long accountId, String email, String role) {

}
