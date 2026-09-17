package com.net2rent.net2rent_backend.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateChecklistItemRequest(
        @NotNull(message = "done es obligatorio")
        Boolean done
) {
}