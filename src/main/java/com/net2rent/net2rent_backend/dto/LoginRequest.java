package com.net2rent.net2rent_backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El formato del email es incorrecto")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        String password
) {
}