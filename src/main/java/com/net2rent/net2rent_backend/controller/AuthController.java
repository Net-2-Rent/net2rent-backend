package com.net2rent.net2rent_backend.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.net2rent.net2rent_backend.dto.LoginRequest;
import com.net2rent.net2rent_backend.dto.LoginResponse;
import com.net2rent.net2rent_backend.security.AuthUser;
import com.net2rent.net2rent_backend.service.AuthService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    @GetMapping("/me")
    public AuthUser me(@AuthenticationPrincipal AuthUser user) {
        return user;
    }
    
    
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
    

}
