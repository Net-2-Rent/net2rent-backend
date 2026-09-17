package com.net2rent.net2rent_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateChecklistItemRequest(
        @NotBlank(message = "El texto no puede estar vacío")
        @Size(max = 200, message = "Máximo 200 caracteres")
        String text
) {
}