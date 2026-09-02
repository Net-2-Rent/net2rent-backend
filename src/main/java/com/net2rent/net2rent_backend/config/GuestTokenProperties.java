package com.net2rent.net2rent_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.guest-token")
public record  GuestTokenProperties (
    String secret,
    long expirationMs
) {

}
