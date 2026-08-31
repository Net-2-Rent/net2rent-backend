package com.net2rent.net2rent_backend.service;

import com.net2rent.net2rent_backend.dto.LodgingResponse;
import com.net2rent.net2rent_backend.exception.NotFoundException;
import com.net2rent.net2rent_backend.repository.LodgingRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LodgingService {

    private final LodgingRepository lodgingRepository;

    public LodgingService(LodgingRepository lodgingRepository) {
        this.lodgingRepository = lodgingRepository;
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
}