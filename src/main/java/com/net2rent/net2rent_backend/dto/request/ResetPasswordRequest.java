package com.net2rent.net2rent_backend.dto.request;

import jakarta.validation.constraints.*;

public record ResetPasswordRequest(
    @NotBlank(message = "La contraseña es obligatoria")
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
        message = "La contraseña debe tener mínimo 8 caracteres, con al menos una letra y un número"
    )
    String password
) {}
