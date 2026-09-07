package com.net2rent.net2rent_backend.dto.request;

import com.net2rent.net2rent_backend.model.enums.UserRole;
import jakarta.validation.constraints.*;

public record CreateUserRequest(
    @NotBlank(message = "El nombre es obligatorio")
    String firstName,

    @NotBlank(message = "Los apellidos son obligatorios")
    String lastName,

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no es válido")
    String email,

    @NotNull(message = "El rol es obligatorio")
    UserRole role,

    @NotBlank(message = "La contraseña es obligatoria")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
        message = "La contraseña debe tener mínimo 8 caracteres, con al menos una letra y un número"
    )
    String password
) {}
