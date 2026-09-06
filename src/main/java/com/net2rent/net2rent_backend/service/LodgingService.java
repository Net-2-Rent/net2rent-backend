package com.net2rent.net2rent_backend.service;

import com.net2rent.net2rent_backend.dto.LodgingResponse;
import com.net2rent.net2rent_backend.dto.request.LodgingRequest;
import com.net2rent.net2rent_backend.exception.ConflictException;
import com.net2rent.net2rent_backend.exception.NotFoundException;
import com.net2rent.net2rent_backend.model.Account;
import com.net2rent.net2rent_backend.model.Lodging;
import com.net2rent.net2rent_backend.repository.LodgingRepository;

import jakarta.persistence.EntityManager;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LodgingService {

    private final LodgingRepository lodgingRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    public LodgingService(LodgingRepository lodgingRepository, PasswordEncoder passwordEncoder,
            EntityManager entityManager) {
        this.lodgingRepository = lodgingRepository;
        this.passwordEncoder = passwordEncoder;
        this.entityManager = entityManager;
    }

    public List<LodgingResponse> listForAccount(Long accountId) {
        return lodgingRepository.findByAccount_Id(accountId)
                .stream()
                .map(LodgingResponse::from)
                .toList();
    }

    public LodgingResponse getForAccount(Long lodgingId, Long accountId) {
        return lodgingRepository.findByIdAndAccount_Id(lodgingId, accountId)
                .map(LodgingResponse::from)
                .orElseThrow(() -> new NotFoundException("Alojamiento no encontrado"));
    }

    @Transactional
    public LodgingResponse create(Long accountId, LodgingRequest request) {
        if (request.pin() == null || request.pin().isBlank()) {
            throw new ConflictException("El PIN es obligatorio al crear un alojamiento");
        }

        assertRefAvailable(request.ref(), null);

        Lodging lodging = new Lodging();
        lodging.setAccount(entityManager.getReference(Account.class, accountId));
        lodging.setRef(request.ref());
        lodging.setName(request.name());
        lodging.setAddress(request.address());
        lodging.setAccessNotes(request.accessNotes());
        lodging.setPinHash(passwordEncoder.encode(request.pin()));
        lodging.setActive(true);

        return LodgingResponse.from(lodgingRepository.save(lodging));
    }

    @Transactional
    public LodgingResponse update(Long accountId, Long id, LodgingRequest request) {
        Lodging lodging = lodgingRepository.findByIdAndAccount_Id(id, accountId)
                .orElseThrow(() -> new NotFoundException("Alojamiento no encontrado"));

        assertRefAvailable(request.ref(), id);

        lodging.setName(request.name());
        lodging.setAddress(request.address());
        lodging.setRef(request.ref());
        lodging.setAccessNotes(request.accessNotes());
        if (request.pin() != null && !request.pin().isBlank()) {
            lodging.setPinHash(passwordEncoder.encode(request.pin()));
        }

        return LodgingResponse.from(lodgingRepository.save(lodging));
    }

    @Transactional
    public void deactivate(Long accountId, Long id) {
        Lodging lodging = lodgingRepository.findByIdAndAccount_Id(id, accountId)
                .orElseThrow(() -> new NotFoundException("Alojamiento no encontrado"));
        lodging.setActive(false);
        lodgingRepository.save(lodging);
    }

    private void assertRefAvailable(String ref, Long currentLodgingId) {
        lodgingRepository.findByRef(ref).ifPresent(existing -> {
            if (currentLodgingId == null || !existing.getId().equals(currentLodgingId)) {
                throw new ConflictException("Ya existe un alojamiento con esa referencia");
            }
        });
    }
}