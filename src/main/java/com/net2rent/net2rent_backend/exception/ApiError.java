package com.net2rent.net2rent_backend.exception;

import java.time.Instant;
import java.util.List;

public record ApiError(
        Instant timestamp,
        int status,
        String path,
        String message,
        List<ApiFieldError> errors
) {
}