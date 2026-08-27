package com.net2rent.net2rent_backend.service;

import java.time.LocalDateTime;
import com.net2rent.net2rent_backend.dto.LoginRequest;
import com.net2rent.net2rent_backend.dto.LoginResponse;
import com.net2rent.net2rent_backend.exception.InvalidCredentialsException;
import com.net2rent.net2rent_backend.model.AppUser;
import com.net2rent.net2rent_backend.repository.UserRepository;
import com.net2rent.net2rent_backend.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        AppUser user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.isActive()) {
            throw new InvalidCredentialsException();
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtService.generateToken(user);
        return new LoginResponse(token, user.getEmail(), user.getFirstName(), user.getRole().name());

                
    }

}
