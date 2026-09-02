package com.net2rent.net2rent_backend.dto.request;

import com.net2rent.net2rent_backend.model.enums.IncidentCategory;
import com.net2rent.net2rent_backend.model.enums.IncidentPriority;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public record CreatePhoneIncidentRequest(
        @NotNull(message = "Selecciona un alojamiento")
        Long lodgingId,

        @NotNull(message = "La fecha de apertura es obligatoria")
        @PastOrPresent(message = "La fecha de apertura no puede ser futura")
        LocalDateTime openedAt,

        @NotBlank(message = "El nombre es obligatorio")
        String firstName,

        @NotBlank(message = "El apellido es obligatorio")
        String lastName,

        @Pattern(
                regexp = "^$|^\\+[1-9]\\d{1,14}$",
                message = "El teléfono debe tener formato internacional, p. ej. +34600111234"
        )
        String contact,

        @NotNull(message = "Selecciona una categoría")
        IncidentCategory category,

        IncidentPriority priority,

        Long assigneeId,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(min = 10, max = 2000, message = "La descripción debe tener entre 10 y 2.000 caracteres")
        String description
) {
}