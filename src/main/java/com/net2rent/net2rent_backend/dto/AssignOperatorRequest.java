package com.net2rent.net2rent_backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AssignOperatorRequest(
    @NotNull(message = "El operario es obligatorio")
    long operatorId,

    @Size(max = 1000, message = "El motivo no puede superar los 1000 caracteres")
    String reason
) {

}
