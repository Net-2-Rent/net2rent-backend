package com.net2rent.net2rent_backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResolveIncidentRequest(
        @NotNull(message = "Debes indicar cuánto tiempo has dedicado")
        @Min(value = 1, message = "El tiempo debe estar entre 1 y 1440 minutos")
        @Max(value = 1440, message = "El tiempo debe estar entre 1 y 1440 minutos")
        Integer minutes,

        @NotBlank(message = "Debes describir cómo se ha resuelto")
        @Size(max = 2000, message = "La nota no puede superar los 2000 caracteres")
        String note
) {
}