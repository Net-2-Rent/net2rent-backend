package com.net2rent.net2rent_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CorrectIncidentTextRequest(

    @Size(max = 150, message = "El título no puede superar los 150 caracteres")
    String title,

    @NotBlank(message = "La descripción es obligatoria")
    String description
) {

}
