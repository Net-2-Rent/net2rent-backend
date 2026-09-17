package com.net2rent.net2rent_backend.dto.request;

import com.net2rent.net2rent_backend.model.enums.UserRole;
import jakarta.validation.constraints.*;

public record UpdateUserRequest(
    
    @NotBlank(message = "El nombre es obligatorio")
    @Pattern(
        regexp = "^[\\p{L} ]+$",
        message = "El nombre solo puede contener letras"
    )
    String firstName,

    @NotBlank(message = "El apellido es obligatorio")
    @Pattern(
        regexp = "^[\\p{L} ]+$",
        message = "El apellido solo puede contener letras"
    )
    String lastName,

    @NotNull(message = "El rol es obligatorio")
    UserRole role,

    @Email(message = "El correo no es válido")
    String email
) {}