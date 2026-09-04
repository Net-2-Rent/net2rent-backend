package com.net2rent.net2rent_backend.dto.request;

import com.net2rent.net2rent_backend.model.enums.IncidentCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateGuestIncidentRequest(

        @NotBlank(message = "El nombre es obligatorio")
        String firstName,

        @NotBlank(message = "El apellido es obligatorio")
        String lastName,

        @Pattern(
                regexp = "^$|^\\+[1-9]\\d{1,14}$",
                message = "El teléfono debe tener formato internacional, p. ej. +34600111234"
        )
        String contact,

        IncidentCategory category,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(min = 10, max = 2000, message = "La descripción debe tener entre 10 y 2.000 caracteres")
        String description
) {
}
