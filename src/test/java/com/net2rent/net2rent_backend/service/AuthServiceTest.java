package com.net2rent.net2rent_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

import com.net2rent.net2rent_backend.dto.ChangePasswordRequest;
import com.net2rent.net2rent_backend.dto.LoginRequest;
import com.net2rent.net2rent_backend.exception.ConflictException;
import com.net2rent.net2rent_backend.exception.InvalidCredentialsException;
import com.net2rent.net2rent_backend.exception.TooManyRequestsException;
import com.net2rent.net2rent_backend.model.AppUser;
import com.net2rent.net2rent_backend.repository.UserRepository;
import com.net2rent.net2rent_backend.security.InMemoryRateLimiter;
import com.net2rent.net2rent_backend.security.JwtService;
import com.net2rent.net2rent_backend.security.RateLimiter;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;

    // Real limiter so the login test exercises the actual wiring.
    private RateLimiter rateLimiter;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        rateLimiter = new InMemoryRateLimiter(
                Clock.fixed(Instant.parse("2026-09-01T10:00:00Z"), ZoneOffset.UTC));
        authService = new AuthService(userRepository, passwordEncoder, jwtService, rateLimiter);
    }

    @Test
    void changePassword_updateHash_whenCurrentIsCorrect() {
        AppUser user = AppUser.builder()
                .id(1L)
                .passwordHash("old-hash")
                .build();

        ChangePasswordRequest request = new ChangePasswordRequest("Test1234", "Nueva1234");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Test1234", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("Nueva1234")).thenReturn("new-hash");

        authService.changePassword(1L, request);

        assertEquals("new-hash", user.getPasswordHash());
        verify(userRepository).save(user);
    }

    @Test
    void changePassword_throwsConflict_whenCurrentIsWrong() {
        AppUser user = AppUser.builder()
                .id(1L)
                .passwordHash("old-hash")
                .build();

        ChangePasswordRequest request = new ChangePasswordRequest("malamala", "Nueva1234");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("malamala", "old-hash")).thenReturn(false);

        assertThrows(ConflictException.class,
                () -> authService.changePassword(1L, request));

        verify(userRepository, never()).save(user);
    }

    @Test
    void changePassword_throwsConflict_whenUserNotFound() {
        ChangePasswordRequest request = new ChangePasswordRequest("Test1234", "Nueva1234");

        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ConflictException.class, () -> authService.changePassword(99L, request));
    }

    @Test
    void login_blocksEmail_afterFiveFailedAttempts() {
        LoginRequest request = new LoginRequest("ana@test.com", "wrong-password");
        when(userRepository.findByEmail("ana@test.com")).thenReturn(Optional.empty());

        // 5 failed attempts: each rejected as invalid credentials.
        for (int i = 0; i < 5; i++) {
            assertThrows(InvalidCredentialsException.class,
                    () -> authService.login(request));
        }

        // 6th attempt for the same email: now blocked (429).
        assertThrows(TooManyRequestsException.class,
                () -> authService.login(request));
    }
}