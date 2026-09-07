package com.net2rent.net2rent_backend.dto.request;

import com.net2rent.net2rent_backend.model.enums.UserRole;
import jakarta.validation.constraints.*;

public record UpdateUserRequest(
    @NotBlank(message = "El nombre es obligatorio")
    String firstName,

    @NotBlank(message = "Los apellidos son obligatorios")
    String lastName,

    @NotNull(message = "El rol es obligatorio")
    UserRole role,

    @Email(message = "El correo no es válido")
    String email
) {}