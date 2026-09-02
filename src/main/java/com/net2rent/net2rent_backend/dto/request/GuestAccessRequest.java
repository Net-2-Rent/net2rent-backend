package com.net2rent.net2rent_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record GuestAccessRequest(

    @NotBlank(message = "La referencia del alojamiento es obligatoria")
    String ref,

    @NotBlank(message = "El PIN es obligatorio")
    @Pattern(regexp = "\\d{4}", message = "El PIN debe tener 4 dígitos")
    String pin
) {    

}
