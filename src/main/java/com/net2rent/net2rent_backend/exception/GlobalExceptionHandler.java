package com.net2rent.net2rent_backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<ApiFieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new ApiFieldError(
                        fieldError.getField(), fieldError.getDefaultMessage()))
                .toList();

        return build(HttpStatus.CONFLICT, "Hay errores de validación", errors, request);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> handleInvalidCredentials(
            InvalidCredentialsException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), List.of(), request);
    }

    @ExceptionHandler(InvalidGuestCredentialsException.class)
    public ResponseEntity<ApiError> handleInvalidGuestCredentials(
            InvalidGuestCredentialsException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), List.of(), request);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            NotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), List.of(), request);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> handleForbidden(
            ForbiddenException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), List.of(), request);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(
            ConflictException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), List.of(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(
            Exception ex, HttpServletRequest request) {
       log.error("Unexpected error handling request {}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ha ocurrido un error inesperado", List.of(), request);
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ApiError> handleTooManyRequests(
            TooManyRequestsException ex, HttpServletRequest request) {
        return build(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), List.of(), request);
    }

    private ResponseEntity<ApiError> build(
            HttpStatus status, String message,
            List<ApiFieldError> errors, HttpServletRequest request) {

        ApiError body = new ApiError(
                Instant.now(), status.value(), request.getRequestURI(), message, errors);
        return ResponseEntity.status(status).body(body);
    }
}