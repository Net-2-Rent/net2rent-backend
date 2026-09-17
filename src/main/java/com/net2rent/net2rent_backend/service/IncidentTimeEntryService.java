package com.net2rent.net2rent_backend.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

import com.net2rent.net2rent_backend.dto.request.CreateTimeEntryRequest;
import com.net2rent.net2rent_backend.dto.response.TimeEntryResponse;
import com.net2rent.net2rent_backend.exception.NotFoundException;
import com.net2rent.net2rent_backend.model.Incident;
import com.net2rent.net2rent_backend.model.IncidentTimeEntry;
import com.net2rent.net2rent_backend.repository.IncidentTimeEntryRepository;
import com.net2rent.net2rent_backend.repository.UserRepository;
import com.net2rent.net2rent_backend.security.AuthUser;
import com.net2rent.net2rent_backend.security.IncidentAccessPolicy;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IncidentTimeEntryService {

    private final IncidentService incidentService;
    private final IncidentAccessPolicy incidentAccessPolicy;
    private final IncidentTimeEntryRepository timeEntryRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public IncidentTimeEntryService(IncidentService incidentService,
            IncidentAccessPolicy incidentAccessPolicy,
            IncidentTimeEntryRepository timeEntryRepository,
            UserRepository userRepository,
            Clock clock) {
        this.incidentService = incidentService;
        this.incidentAccessPolicy = incidentAccessPolicy;
        this.timeEntryRepository = timeEntryRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<TimeEntryResponse> list(Long incidentId, AuthUser user) {
        Incident incident = incidentService.getOwnedByAccountOr404(incidentId, user);
        return timeEntryRepository
                .findByIncident_IdOrderByCreatedAtAscIdAsc(incident.getId())
                .stream()
                .map(TimeEntryResponse::from)
                .toList();
    }

    @Transactional
    public List<TimeEntryResponse> add(Long incidentId, CreateTimeEntryRequest request, AuthUser user) {
        Incident incident = incidentService.getOwnedByAccountOr404(incidentId, user);
        incidentAccessPolicy.ensureCanActOn(incident, user);
        incidentAccessPolicy.ensureNotTerminal(incident);

        IncidentTimeEntry entry = IncidentTimeEntry.builder()
                .incident(incident)
                .author(userRepository.getReferenceById(user.userId()))
                .concept(request.concept().trim())
                .minutes(request.minutes())
                .createdAt(LocalDateTime.now(clock))
                .build();

        timeEntryRepository.save(entry);

        return list(incidentId, user);
    }

    @Transactional
    public List<TimeEntryResponse> update(Long incidentId, Long entryId,
            CreateTimeEntryRequest request, AuthUser user) {
        Incident incident = incidentService.getOwnedByAccountOr404(incidentId, user);
        incidentAccessPolicy.ensureCanActOn(incident, user);
        incidentAccessPolicy.ensureNotTerminal(incident);

        IncidentTimeEntry entry = timeEntryRepository
                .findByIdAndIncident_Id(entryId, incident.getId())
                .orElseThrow(() -> new NotFoundException("Imputación no encontrada"));

        entry.setConcept(request.concept().trim());
        entry.setMinutes(request.minutes());
        timeEntryRepository.save(entry);

        return list(incidentId, user);
    }

    @Transactional
    public List<TimeEntryResponse> delete(Long incidentId, Long entryId, AuthUser user) {
        Incident incident = incidentService.getOwnedByAccountOr404(incidentId, user);
        incidentAccessPolicy.ensureCanActOn(incident, user);
        incidentAccessPolicy.ensureNotTerminal(incident);

        IncidentTimeEntry entry = timeEntryRepository
                .findByIdAndIncident_Id(entryId, incident.getId())
                .orElseThrow(() -> new NotFoundException("Imputación no encontrada"));

        timeEntryRepository.delete(entry);
        return list(incidentId, user);
    }

    

}
