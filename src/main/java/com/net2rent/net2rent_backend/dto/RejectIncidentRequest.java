package com.net2rent.net2rent_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectIncidentRequest(
    @NotBlank(message = "El motivo del rechazo es obligatorio")
    @Size(max = 1000, message = "El motivo no puede superar los 1000 caracteres")
    String reason
) {

}
