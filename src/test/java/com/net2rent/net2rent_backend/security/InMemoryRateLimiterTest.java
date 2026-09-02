package com.net2rent.net2rent_backend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRateLimiterTest {

    private static final String KEY = "user@test.com";
    private static final Instant START = Instant.parse("2026-09-01T10:00:00Z");

    private MutableClock clock;
    private InMemoryRateLimiter limiter;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(START, ZoneOffset.UTC);
        limiter = new InMemoryRateLimiter(clock);
    }

    @Test
    void unknownKeyIsNotBlocked() {
        assertThat(limiter.isBlocked("never-seen")).isFalse();
    }

    @Test
    void isNotBlockedBeforeReachingTheLimit() {
        registerFailures(KEY, 4);
        assertThat(limiter.isBlocked(KEY)).isFalse();
    }

    @Test
    void isBlockedWhenTheLimitIsReached() {
        registerFailures(KEY, 5);
        assertThat(limiter.isBlocked(KEY)).isTrue();
    }

    @Test
    void resetClearsTheCounter() {
        registerFailures(KEY, 5);
        assertThat(limiter.isBlocked(KEY)).isTrue();

        limiter.reset(KEY);

        assertThat(limiter.isBlocked(KEY)).isFalse();
    }

    @Test
    void windowExpiresAfterFifteenMinutes() {
        registerFailures(KEY, 5);
        assertThat(limiter.isBlocked(KEY)).isTrue();

        clock.advance(Duration.ofMinutes(15).plusSeconds(1));

        assertThat(limiter.isBlocked(KEY)).isFalse();
    }

    @Test
    void stillBlockedInsideTheWindow() {
        registerFailures(KEY, 5);

        clock.advance(Duration.ofMinutes(14));

        assertThat(limiter.isBlocked(KEY)).isTrue();
    }

    private void registerFailures(String key, int times) {
        for (int i = 0; i < times; i++) {
            limiter.registerFailure(key);
        }
    }

    private static class MutableClock extends Clock {
        private Instant instant;
        private final ZoneId zone;

        MutableClock(Instant instant, ZoneId zone) {
            this.instant = instant;
            this.zone = zone;
        }

        void advance(Duration amount) {
            this.instant = this.instant.plus(amount);
        }

        @Override
        public Instant instant() {
            return instant;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant, zone);
        }
    }
}