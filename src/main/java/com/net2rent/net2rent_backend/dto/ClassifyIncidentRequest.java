package com.net2rent.net2rent_backend.dto;

import com.net2rent.net2rent_backend.model.enums.IncidentCategory;
import com.net2rent.net2rent_backend.model.enums.IncidentPriority;

import jakarta.validation.constraints.NotNull;

public record ClassifyIncidentRequest(

    @NotNull(message = "La categoría es obligatoria")
    IncidentCategory category,

    @NotNull(message = "La prioridad es obligatoria")
    IncidentPriority priority
) {

}
