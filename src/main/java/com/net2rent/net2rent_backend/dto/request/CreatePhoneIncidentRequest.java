package com.net2rent.net2rent_backend.dto.request;

import com.net2rent.net2rent_backend.model.enums.IncidentCategory;
import com.net2rent.net2rent_backend.model.enums.IncidentPriority;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.List;

public record CreatePhoneIncidentRequest(
        @NotNull(message = "Selecciona un alojamiento")
        Long lodgingId,

        @NotNull(message = "La fecha de apertura es obligatoria")
        @PastOrPresent(message = "La fecha de apertura no puede ser futura")
        LocalDateTime openedAt,

        @NotBlank(message = "El nombre es obligatorio")
        @Pattern(
                regexp = "^[\\p{L}\\p{M} '-]+$",
                message = "El nombre no puede contener números"
        )
        String firstName,

        @NotBlank(message = "El apellido es obligatorio")
        @Pattern(
                regexp = "^[\\p{L}\\p{M} '-]+$",
                message = "El apellido no puede contener números"
        )
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
        String description,

        @Size(max = 3, message = "Solo se pueden adjuntar hasta 3 imágenes")
        List<String> images
) {
}