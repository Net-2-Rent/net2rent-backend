package com.net2rent.net2rent_backend.dto;

import com.net2rent.net2rent_backend.model.AppUser;

public record OperatorResponse(Long id, String name) {
    public static OperatorResponse from(AppUser u) {
        return new OperatorResponse(u.getId(), u.getFirstName() + " " + u.getLastName());
    }
}