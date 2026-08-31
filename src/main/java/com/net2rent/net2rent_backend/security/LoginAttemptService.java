package com.net2rent.net2rent_backend.security;

import com.net2rent.net2rent_backend.exception.TooManyRequestsException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();

    public void assertNotBlocked(String email) {
        Attempt attempt = attempts.get(key(email));
        if (attempt != null
                && attempt.count() >= MAX_ATTEMPTS
                && Instant.now().isBefore(attempt.lockedUntil())) {
            throw new TooManyRequestsException(
                    "Demasiados intentos. Inténtalo de nuevo más tarde.");
        }
    }

    public void loginFailed(String email) {
        attempts.compute(key(email), (k, current) -> {
            Instant now = Instant.now();
            int base = (current == null || now.isAfter(current.lockedUntil())) ? 0 : current.count();
            return new Attempt(base + 1, now);
        });
    }

    public void loginSucceeded(String email) {
        attempts.remove(key(email));
    }

    private String key(String email) {
        return email == null ? "" : email.toLowerCase();
    }

    private record Attempt(int count, Instant lastFailedAt) {
        Instant lockedUntil() {
            return lastFailedAt.plus(LOCK_DURATION);
        }
    }
}