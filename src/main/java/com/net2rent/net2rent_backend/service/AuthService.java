package com.net2rent.net2rent_backend.service;

import java.time.LocalDateTime;
import java.time.Clock;

import com.net2rent.net2rent_backend.dto.request.ChangePasswordRequest;
import com.net2rent.net2rent_backend.exception.ConflictException;
import com.net2rent.net2rent_backend.dto.request.LoginRequest;
import com.net2rent.net2rent_backend.dto.response.LoginResponse;
import com.net2rent.net2rent_backend.exception.InvalidCredentialsException;
import com.net2rent.net2rent_backend.exception.TooManyRequestsException;
import com.net2rent.net2rent_backend.model.AppUser;
import com.net2rent.net2rent_backend.repository.UserRepository;
import com.net2rent.net2rent_backend.security.JwtService;
import com.net2rent.net2rent_backend.security.RateLimiter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RateLimiter rateLimiter;
    private final Clock clock;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       RateLimiter rateLimiter,
                       Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.rateLimiter = rateLimiter;
        this.clock = clock;
    }

    @Transactional
    public LoginResponse login(LoginRequest request, String clientIp) {

        String key = clientIp;

        if (rateLimiter.isBlocked(key)) {
            throw new TooManyRequestsException();
        }

        AppUser user = userRepository.findByEmail(request.email()).orElse(null);

        boolean valid = user != null
                && user.isActive()
                && passwordEncoder.matches(request.password(), user.getPasswordHash());

        if (!valid) {
            rateLimiter.registerFailure(key);
            throw new InvalidCredentialsException();
        }

        rateLimiter.reset(key);

        user.setLastLoginAt(LocalDateTime.now(clock));
        userRepository.save(user);

        String token = jwtService.generateToken(user);
        return new LoginResponse(token, user.getEmail(), user.getFirstName(), user.getLastName(), user.getRole().name());
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        AppUser user = userRepository.findById(userId)
        .orElseThrow(() -> new ConflictException("No se pudo cambiar la contraseña."));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ConflictException("La contraseña actual no es correcta.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }
}