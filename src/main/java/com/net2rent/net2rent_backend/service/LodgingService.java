package com.net2rent.net2rent_backend.service;

import com.net2rent.net2rent_backend.dto.LodgingResponse;
import com.net2rent.net2rent_backend.dto.request.LodgingRequest;
import com.net2rent.net2rent_backend.exception.ConflictException;
import com.net2rent.net2rent_backend.exception.NotFoundException;
import com.net2rent.net2rent_backend.model.Account;
import com.net2rent.net2rent_backend.model.Lodging;
import com.net2rent.net2rent_backend.model.LodgingCounter;
import com.net2rent.net2rent_backend.repository.LodgingCounterRepository;
import com.net2rent.net2rent_backend.repository.LodgingRepository;

import jakarta.persistence.EntityManager;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LodgingService {

    private final LodgingRepository lodgingRepository;
    private final LodgingCounterRepository lodgingCounterRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    public LodgingService(LodgingRepository lodgingRepository,
            LodgingCounterRepository lodgingCounterRepository,
            PasswordEncoder passwordEncoder,
            EntityManager entityManager) {
        this.lodgingRepository = lodgingRepository;
        this.lodgingCounterRepository = lodgingCounterRepository;
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

        Lodging lodging = new Lodging();
        lodging.setAccount(entityManager.getReference(Account.class, accountId));
        lodging.setRef(nextLodgingRef());
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

        lodging.setName(request.name());
        lodging.setAddress(request.address());
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

    private String nextLodgingRef() {
        LodgingCounter counter = lodgingCounterRepository.findForUpdate()
                .orElseGet(() -> lodgingCounterRepository.save(
                        LodgingCounter.builder().lastNumber(0).build()));

        counter.setLastNumber(counter.getLastNumber() + 1);
        lodgingCounterRepository.save(counter);

        return "APT-%04d".formatted(counter.getLastNumber());
    }
}