package com.net2rent.net2rent_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.net2rent.net2rent_backend.dto.request.GuestAccessRequest;
import com.net2rent.net2rent_backend.dto.response.GuestAccessResponse;
import com.net2rent.net2rent_backend.exception.InvalidGuestCredentialsException;
import com.net2rent.net2rent_backend.exception.TooManyRequestsException;
import com.net2rent.net2rent_backend.model.Lodging;
import com.net2rent.net2rent_backend.repository.LodgingRepository;
import com.net2rent.net2rent_backend.security.GuestTokenService;
import com.net2rent.net2rent_backend.security.InMemoryRateLimiter;
import com.net2rent.net2rent_backend.security.RateLimiter;

@ExtendWith(MockitoExtension.class)
class GuestAuthServiceTest {

    private static final String IP = "10.0.0.1";

    @Mock
    private LodgingRepository lodgingRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private GuestTokenService guestTokenService;

    private RateLimiter rateLimiter;
    private GuestAuthService guestAuthService;

    @BeforeEach
    void setUp() {
        // Fixed clock: these tests don't need to advance time.
        rateLimiter = new InMemoryRateLimiter(
                Clock.fixed(Instant.parse("2026-09-01T10:00:00Z"), ZoneOffset.UTC));
        guestAuthService = new GuestAuthService(
                lodgingRepository, passwordEncoder, guestTokenService, rateLimiter);
    }

    @Test
    void access_returnsToken_whenPinIsCorrect() {
        Lodging lodging = Lodging.builder()
                .id(1L)
                .ref("APT-1001")
                .pinHash("hashed-pin")
                .name("Apartamento Centro")
                .active(true)
                .build();

        GuestAccessRequest request = new GuestAccessRequest("APT-1001", "1234");

        when(lodgingRepository.findByRef("APT-1001"))
                .thenReturn(Optional.of(lodging));
        when(passwordEncoder.matches("1234", "hashed-pin"))
                .thenReturn(true);
        when(guestTokenService.generateToken(lodging))
                .thenReturn("fake-jwt-token");

        GuestAccessResponse response = guestAuthService.access(request, IP);

        assertEquals("fake-jwt-token", response.token());
        assertEquals(1L, response.lodgingId());
        assertEquals("Apartamento Centro", response.lodgingName());
    }

    @Test
    void access_throwsInvalidGuestCredentials_whenPinIsWrong() {
        Lodging lodging = Lodging.builder()
                .id(1L)
                .ref("APT-1001")
                .pinHash("hashed-pin")
                .name("Apartamento Centro")
                .active(true)
                .build();

        GuestAccessRequest request = new GuestAccessRequest("APT-1001", "9999");

        when(lodgingRepository.findByRef("APT-1001"))
                .thenReturn(Optional.of(lodging));
        when(passwordEncoder.matches("9999", "hashed-pin"))
                .thenReturn(false);

        assertThrows(InvalidGuestCredentialsException.class,
                () -> guestAuthService.access(request, IP));
    }

    @Test
    void access_blocksIp_afterFiveFailedAttempts() {
        GuestAccessRequest request = new GuestAccessRequest("APT-404", "0000");
        when(lodgingRepository.findByRef("APT-404")).thenReturn(Optional.empty());

        // 5 failed attempts: each rejected as invalid credentials (would map to 401).
        for (int i = 0; i < 5; i++) {
            assertThrows(InvalidGuestCredentialsException.class,
                    () -> guestAuthService.access(request, IP));
        }

        // 6th attempt from the same IP: now blocked (would map to 429).
        assertThrows(TooManyRequestsException.class,
                () -> guestAuthService.access(request, IP));
    }
}