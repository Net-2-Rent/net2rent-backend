package com.net2rent.net2rent_backend.service;

import java.time.LocalDateTime;
import com.net2rent.net2rent_backend.dto.LoginRequest;
import com.net2rent.net2rent_backend.dto.LoginResponse;
import com.net2rent.net2rent_backend.exception.InvalidCredentialsException;
import com.net2rent.net2rent_backend.model.AppUser;
import com.net2rent.net2rent_backend.repository.UserRepository;
import com.net2rent.net2rent_backend.security.JwtService;
import com.net2rent.net2rent_backend.security.LoginAttemptService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       LoginAttemptService loginAttemptService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.loginAttemptService = loginAttemptService;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String email = request.email();

        loginAttemptService.assertNotBlocked(email);

        AppUser user = userRepository.findByEmail(email).orElse(null);

        boolean valid = user != null
                && user.isActive()
                && passwordEncoder.matches(request.password(), user.getPasswordHash());

        if (!valid) {
            loginAttemptService.loginFailed(email);
            throw new InvalidCredentialsException();
        }

        loginAttemptService.loginSucceeded(email);

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtService.generateToken(user);
        return new LoginResponse(token, user.getEmail(), user.getFirstName(), user.getLastName(), user.getRole().name());
    }
}