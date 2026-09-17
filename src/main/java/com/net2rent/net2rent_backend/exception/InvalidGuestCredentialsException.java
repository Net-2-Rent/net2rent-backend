package com.net2rent.net2rent_backend.exception;

public class InvalidGuestCredentialsException extends RuntimeException {

    public InvalidGuestCredentialsException() {
        super("Referencia o PIN incorrectos");
    }

}
