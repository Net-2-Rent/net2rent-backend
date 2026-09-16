package com.net2rent.net2rent_backend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateTimeEntryRequest(
    @NotBlank(message = "El concepto no puede estar vacío")
    @Size(max = 100, message= "Máximo 100 caracteres")
    String concept,

    @NotNull(message = "Los minutos son obligatorios")
    @Positive(message = "Los minutosd deben ser mayores que 0")
    @Max(value = 1440, message = "Máximo 1440 minutos (24 h)")
    Integer minutes
) {

}
