package com.net2rent.net2rent_backend.dto.response;

import com.net2rent.net2rent_backend.model.AppUser;
import com.net2rent.net2rent_backend.model.enums.UserRole;

public record UserResponse(Long id,
        String firstName,
        String lastName,
        String email,
        UserRole role,
        boolean active) {
    public static UserResponse from(AppUser u) {
        return new UserResponse(u.getId(), u.getFirstName(), u.getLastName(),
                u.getEmail(), u.getRole(), u.isActive());
    }

}
