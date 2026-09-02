package com.net2rent.net2rent_backend.service;

import com.net2rent.net2rent_backend.dto.IncidentResponse;
import com.net2rent.net2rent_backend.dto.response.GuestIncidentDetailResponse;
import com.net2rent.net2rent_backend.dto.response.GuestIncidentSummaryResponse;
import com.net2rent.net2rent_backend.exception.NotFoundException;
import com.net2rent.net2rent_backend.model.Incident;
import com.net2rent.net2rent_backend.model.enums.UserRole;
import com.net2rent.net2rent_backend.repository.IncidentRepository;
import com.net2rent.net2rent_backend.security.AuthUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;

    public IncidentService(IncidentRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
    }

    @Transactional(readOnly = true)
    public List<IncidentResponse> list(AuthUser user) {
        List<Incident> incidents;

        if (UserRole.OPERATOR.name().equals(user.role())) {
            incidents = incidentRepository.findVisibleToOperator(
                    user.accountId(), user.userId());
        } else {
            incidents = incidentRepository.findByAccount_Id(user.accountId());
        }

        return incidents.stream().map(IncidentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public Incident getOwnedByAccountOr404(Long incidentId, AuthUser user) {
        return incidentRepository
                .findByIdAndAccount_Id(incidentId, user.accountId())
                .orElseThrow(() -> new NotFoundException("Incidencia no encontrada"));
    }

    @Transactional(readOnly = true)
    public List<GuestIncidentSummaryResponse> listByLodging(Long lodgingId) {
        return incidentRepository.findByLodging_IdOrderByOpenedAtDesc(lodgingId)
                .stream()
                .map(GuestIncidentSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Incident getOwnedByLodgingOr404(Long incidentId, Long lodgingId) {
        return incidentRepository.findByIdAndLodging_Id(incidentId, lodgingId)
                .orElseThrow(() -> new NotFoundException("Incidencia no encontrada"));
    }
}