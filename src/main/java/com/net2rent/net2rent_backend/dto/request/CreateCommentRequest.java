package com.net2rent.net2rent_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest (
    @NotBlank(message = "El comentario no puede estar vacío")
    @Size(max = 2000, message = "Máximo 2.000 caracteres")
    String text
) {
}