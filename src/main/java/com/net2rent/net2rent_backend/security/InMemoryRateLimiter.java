package com.net2rent.net2rent_backend.security;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryRateLimiter implements RateLimiter {

    private static final int MAX_FAILURES = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private final Clock clock;

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public InMemoryRateLimiter(Clock clock) {
        this.clock = clock;
    }

    @Override
    public boolean isBlocked(String key) {
        Window window = windows.get(key);
        if (window == null) {
            return false;
        }
        if (window.isExpired(Instant.now(clock), WINDOW)) {
            return false;
        }
        return window.failures() >= MAX_FAILURES;
    }

    @Override
    public void registerFailure(String key) {
        Instant now = Instant.now(clock);
        windows.compute(key, (ignoredKey, existingWindow) -> {
            if (existingWindow == null || existingWindow.isExpired(now, WINDOW)) {
                return new Window(now, 1);
            }
            return existingWindow.increment();
        });
    }

    @Override
    public void reset(String key) {
        windows.remove(key);
    }

    private record Window(Instant startedAt, int failures) {

        boolean isExpired(Instant now, Duration window) {
            return now.isAfter(startedAt.plus(window));
        }

        Window increment() {
            return new Window(startedAt, failures + 1);
        }
    }
}