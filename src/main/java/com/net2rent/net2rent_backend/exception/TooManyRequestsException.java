package com.net2rent.net2rent_backend.exception;

public class TooManyRequestsException extends RuntimeException {

    private static final String DEFAULT_MESSAGE =
            "Demasiados intentos. Vuelve a probarlo más tarde.";

    public TooManyRequestsException() {
        super(DEFAULT_MESSAGE);
    }

    public TooManyRequestsException(String message) {
        super(message);
    }
}