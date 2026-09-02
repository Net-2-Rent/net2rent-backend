package com.net2rent.net2rent_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.net2rent.net2rent_backend.dto.request.GuestAccessRequest;
import com.net2rent.net2rent_backend.dto.response.GuestAccessResponse;
import com.net2rent.net2rent_backend.exception.InvalidGuestCredentialsException;
import com.net2rent.net2rent_backend.model.Lodging;
import com.net2rent.net2rent_backend.repository.LodgingRepository;
import com.net2rent.net2rent_backend.security.GuestTokenService;

@ExtendWith(MockitoExtension.class)
class GuestAuthServiceTest {

    @Mock
    private LodgingRepository lodgingRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private GuestTokenService guestTokenService;

    @InjectMocks
    private GuestAuthService guestAuthService;

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

        when(lodgingRepository.findByRef("APT-1001")).thenReturn(Optional.of(lodging));
        when(passwordEncoder.matches("1234", "hashed-pin")).thenReturn(true);
        when(guestTokenService.generateToken(lodging)).thenReturn("fake-jwt-token");

        GuestAccessResponse response = guestAuthService.access(request);

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

        when(lodgingRepository.findByRef("APT-1001")).thenReturn(Optional.of(lodging));
        when(passwordEncoder.matches("9999", "hashed-pin")).thenReturn(false);

        assertThrows(InvalidGuestCredentialsException.class,
                () -> guestAuthService.access(request));
    }

}
