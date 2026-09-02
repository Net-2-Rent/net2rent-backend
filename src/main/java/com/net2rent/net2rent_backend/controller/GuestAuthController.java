package com.net2rent.net2rent_backend.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

import com.net2rent.net2rent_backend.dto.request.GuestAccessRequest;
import com.net2rent.net2rent_backend.dto.response.GuestAccessResponse;
import com.net2rent.net2rent_backend.service.GuestAuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/guest")
public class GuestAuthController {

    private final GuestAuthService guestAuthService;

    public GuestAuthController(GuestAuthService guestAuthService) {
        this.guestAuthService = guestAuthService;
    }

    @PostMapping("/access")
    public GuestAccessResponse access(@Valid @RequestBody GuestAccessRequest request) {
        return guestAuthService.access(request);
    }
}
