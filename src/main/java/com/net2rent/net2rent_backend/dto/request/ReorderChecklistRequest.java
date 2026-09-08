package com.net2rent.net2rent_backend.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

public record ReorderChecklistRequest(
    @NotEmpty(message = "La lista de orden no puede estar vacía")
    List<Long> orderedIds
) {}
