package com.net2rent.net2rent_backend.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.net2rent.net2rent_backend.dto.request.GuestAccessRequest;
import com.net2rent.net2rent_backend.dto.response.GuestAccessResponse;
import com.net2rent.net2rent_backend.exception.InvalidGuestCredentialsException;
import com.net2rent.net2rent_backend.model.Lodging;
import com.net2rent.net2rent_backend.repository.LodgingRepository;
import com.net2rent.net2rent_backend.security.GuestTokenService;


@Service
public class GuestAuthService {

      private static final long EXPIRES_IN_SECONDS = 2 * 60 * 60; // 2h, igual que GuestTokenProperties

    private final LodgingRepository lodgingRepository;
    private final PasswordEncoder passwordEncoder;
    private final GuestTokenService guestTokenService;

    public GuestAuthService(LodgingRepository lodgingRepository,
                             PasswordEncoder passwordEncoder,
                             GuestTokenService guestTokenService) {
        this.lodgingRepository = lodgingRepository;
        this.passwordEncoder = passwordEncoder;
        this.guestTokenService = guestTokenService;
    }

    @Transactional(readOnly = true)
    public GuestAccessResponse access(GuestAccessRequest request) {
        Lodging lodging = lodgingRepository.findByRef(request.ref())
                .orElseThrow(InvalidGuestCredentialsException::new);

        if (!lodging.isActive()) {
            throw new InvalidGuestCredentialsException();
        }

        if (!passwordEncoder.matches(request.pin(), lodging.getPinHash())) {
            throw new InvalidGuestCredentialsException();
        }

        String token = guestTokenService.generateToken(lodging);
        return new GuestAccessResponse(token, lodging.getId(), lodging.getName(), EXPIRES_IN_SECONDS);
    }

}
