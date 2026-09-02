package com.net2rent.net2rent_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;


public record LodgingRequest(

    @NotBlank(message = "El nombre es obligatorio")
    String name,

    @NotBlank(message = "La dirección es obligatoria")
    String address,

    @NotBlank(message = "La referencia es obligatoria")
    String ref,

    @Pattern(regexp = "\\d{4}", message = "El PIN debe tener 4 dígitos")
    String pin,

    String accessNotes
){

}

