package com.net2rent.net2rent_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.net2rent.net2rent_backend.dto.ChangePasswordRequest;
import com.net2rent.net2rent_backend.exception.ConflictException;
import com.net2rent.net2rent_backend.model.AppUser;
import com.net2rent.net2rent_backend.repository.UserRepository;
import com.net2rent.net2rent_backend.security.JwtService;
import com.net2rent.net2rent_backend.security.LoginAttemptService;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private LoginAttemptService loginAttemptService;

    @InjectMocks private AuthService authService;

    @Test
    void changepassword_updateHash_whenCurrentIsCorrect() {
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
}