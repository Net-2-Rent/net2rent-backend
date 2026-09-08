package com.net2rent.net2rent_backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.persistence.EntityManager;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.net2rent.net2rent_backend.exception.ConflictException;
import com.net2rent.net2rent_backend.exception.NotFoundException;

import com.net2rent.net2rent_backend.dto.LodgingResponse;
import com.net2rent.net2rent_backend.dto.request.LodgingRequest;
import com.net2rent.net2rent_backend.model.Account;
import com.net2rent.net2rent_backend.model.Lodging;
import com.net2rent.net2rent_backend.repository.LodgingRepository;

import com.net2rent.net2rent_backend.model.LodgingCounter;
import com.net2rent.net2rent_backend.repository.LodgingCounterRepository;

@ExtendWith(MockitoExtension.class)
class LodgingServiceTest {

    @Mock
    private LodgingRepository lodgingRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EntityManager entityManager;
    @Mock
    private LodgingCounterRepository lodgingCounterRepository;

    private LodgingService lodgingService;

    @BeforeEach
    void setUp() {
        lodgingService = new LodgingService(lodgingRepository, lodgingCounterRepository, passwordEncoder, entityManager);
    }

        @Test
    void create_savesLodging_generatesSequentialRef_whenPinIsValid() {
        Long accountId = 1L;
        LodgingRequest request = new LodgingRequest(
                "Apto Centro",
                "Calle Mayor 5",
                "1234",
                "Notas de acceso");

        Account account = Account.builder().id(accountId).build();
        LodgingCounter counter = LodgingCounter.builder().id(1L).lastNumber(0).build();

        when(lodgingCounterRepository.findForUpdate()).thenReturn(Optional.of(counter));
        when(lodgingCounterRepository.save(any(LodgingCounter.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(entityManager.getReference(Account.class, accountId)).thenReturn(account);
        when(passwordEncoder.encode("1234")).thenReturn("hashed-pin");
        when(lodgingRepository.save(any(Lodging.class))).thenAnswer(invocation -> {
            Lodging saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        LodgingResponse response = lodgingService.create(accountId, request);

        assertEquals(10L, response.id());
        assertEquals("APT-0001", response.ref());
        assertEquals("Apto Centro", response.name());
        assertTrue(response.active());
    }

    @Test
    void create_throwsConflict_whenPinIsBlank() {
        Long accountId = 1L;
        LodgingRequest request = new LodgingRequest(
                "Apto Centro",
                "Calle Mayor 5",
                "",
                "Notas de acceso");

        assertThrows(ConflictException.class,
                () -> lodgingService.create(accountId, request));

        verify(lodgingCounterRepository, never()).findForUpdate();
        verify(lodgingRepository, never()).save(any(Lodging.class));
    }

    @Test
    void update_updatesLodging_whenFoundAndRefIsAvailable() {
        Long accountId = 1L;
        Long lodgingId = 5L;

        Lodging existing = Lodging.builder()
                .id(lodgingId)
                .ref("APT-1001")
                .name("Nombre viejo")
                .address("Dirección vieja")
                .pinHash("old-hash")
                .active(true)
                .build();

        LodgingRequest request = new LodgingRequest(
                "Nombre nuevo",
                "Dirección nueva",
                "5678",
                "Notas nuevas");

        when(lodgingRepository.findByIdAndAccount_Id(lodgingId, accountId))
                .thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("5678")).thenReturn("new-hash");
        when(lodgingRepository.save(any(Lodging.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LodgingResponse response = lodgingService.update(accountId, lodgingId, request);

        assertEquals("Nombre nuevo", response.name());
        assertEquals("Dirección nueva", response.address());
        assertEquals("new-hash", existing.getPinHash());
    }

    @Test
    void update_throwsNotFound_whenLodgingDoesNotExist() {
        Long accountId = 1L;
        Long lodgingId = 99L;

        LodgingRequest request = new LodgingRequest(
                "Nombre nuevo",
                "Dirección nueva",
                "5678",
                "Notas nuevas");

        when(lodgingRepository.findByIdAndAccount_Id(lodgingId, accountId))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> lodgingService.update(accountId, lodgingId, request));

        verify(lodgingRepository, never()).save(any(Lodging.class));
    }


    @Test
    void deactivate_setsActiveFalse_whenLodgingExists() {
        Long accountId = 1L;
        Long lodgingId = 5L;

        Lodging existing = Lodging.builder()
                .id(lodgingId)
                .active(true)
                .build();

        when(lodgingRepository.findByIdAndAccount_Id(lodgingId, accountId))
                .thenReturn(Optional.of(existing));

        lodgingService.deactivate(accountId, lodgingId);

        assertFalse(existing.isActive());
        verify(lodgingRepository).save(existing);
    }

    @Test
    void deactivate_throwsNotFound_whenLodgingDoesNotExist() {
        Long accountId = 1L;
        Long lodgingId = 99L;

        when(lodgingRepository.findByIdAndAccount_Id(lodgingId, accountId))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> lodgingService.deactivate(accountId, lodgingId));

        verify(lodgingRepository, never()).save(any(Lodging.class));
    }
}
