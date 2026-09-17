package com.net2rent.net2rent_backend.service;

import com.net2rent.net2rent_backend.exception.TooManyRequestsException;
import com.net2rent.net2rent_backend.security.RateLimiter;
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

    private static final long EXPIRES_IN_SECONDS = 2 * 60 * 60;

    private final LodgingRepository lodgingRepository;
    private final PasswordEncoder passwordEncoder;
    private final GuestTokenService guestTokenService;
    private final RateLimiter rateLimiter;

    public GuestAuthService(LodgingRepository lodgingRepository,
                            PasswordEncoder passwordEncoder,
                            GuestTokenService guestTokenService,
                            RateLimiter rateLimiter) {
        this.lodgingRepository = lodgingRepository;
        this.passwordEncoder = passwordEncoder;
        this.guestTokenService = guestTokenService;
        this.rateLimiter = rateLimiter;
    }

    @Transactional(readOnly = true)
    public GuestAccessResponse access(GuestAccessRequest request, String clientIp) {

        if (rateLimiter.isBlocked(clientIp)) {
            throw new TooManyRequestsException();
        }

        Lodging lodging = lodgingRepository.findByRef(request.ref()).orElse(null);
        boolean valid = lodging != null
                && lodging.isActive()
                && passwordEncoder.matches(request.pin(), lodging.getPinHash());

        if (!valid) {
            rateLimiter.registerFailure(clientIp);
            throw new InvalidGuestCredentialsException();
        }

        rateLimiter.reset(clientIp);

        String token = guestTokenService.generateToken(lodging);
        return new GuestAccessResponse(token, lodging.getId(), lodging.getName(), lodging.getRef(), lodging.getAddress(), EXPIRES_IN_SECONDS);
    }
}