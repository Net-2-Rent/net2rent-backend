package com.net2rent.net2rent_backend.service;

import com.net2rent.net2rent_backend.dto.IncidentResponse;
import com.net2rent.net2rent_backend.dto.PauseIncidentRequest;
import com.net2rent.net2rent_backend.exception.ConflictException;
import com.net2rent.net2rent_backend.model.AppUser;
import com.net2rent.net2rent_backend.model.Incident;
import com.net2rent.net2rent_backend.model.enums.IncidentEventType;
import com.net2rent.net2rent_backend.model.enums.IncidentStatus;
import com.net2rent.net2rent_backend.repository.IncidentRepository;
import com.net2rent.net2rent_backend.repository.UserRepository;
import com.net2rent.net2rent_backend.security.AuthUser;
import com.net2rent.net2rent_backend.security.IncidentAccessPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class IncidentExecutionService {

    private final IncidentService incidentService;
    private final IncidentAccessPolicy incidentAccessPolicy;
    private final IncidentRepository incidentRepository;
    private final IncidentHistoryService incidentHistoryService;
    private final UserRepository userRepository;
    private final Clock clock;

    public IncidentExecutionService(IncidentService incidentService,
                                    IncidentAccessPolicy incidentAccessPolicy,
                                    IncidentRepository incidentRepository,
                                    IncidentHistoryService incidentHistoryService,
                                    UserRepository userRepository,
                                    Clock clock) {
        this.incidentService = incidentService;
        this.incidentAccessPolicy = incidentAccessPolicy;
        this.incidentRepository = incidentRepository;
        this.incidentHistoryService = incidentHistoryService;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    // ---------- CU-EXE-03: start ----------

    @Transactional
    public IncidentResponse start(Long incidentId, AuthUser user) {
        Incident incident = incidentService.getOwnedByAccountOr404(incidentId, user);
        incidentAccessPolicy.ensureCanActOn(incident, user);

        IncidentStatus current = incident.getStatus();
        if (current != IncidentStatus.ASSIGNED) {
            throw new ConflictException("No se puede comenzar una incidencia en estado " + current);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        AppUser actor = userRepository.getReferenceById(user.userId());

        incident.setStatus(IncidentStatus.IN_PROGRESS);
        incident.setStartedAt(now);

        incidentHistoryService.record(incident, actor, IncidentEventType.STATUS_CHANGED,
                current.name(), IncidentStatus.IN_PROGRESS.name(), now);

        incidentRepository.save(incident);
        return IncidentResponse.from(incident);
    }

    // ---------- CU-EXE-04: pause ----------

    @Transactional
    public IncidentResponse pause(Long incidentId, PauseIncidentRequest request, AuthUser user) {
        Incident incident = incidentService.getOwnedByAccountOr404(incidentId, user);
        incidentAccessPolicy.ensureCanActOn(incident, user);

        IncidentStatus current = incident.getStatus();
        if (current != IncidentStatus.IN_PROGRESS) {
            throw new ConflictException("No se puede pausar una incidencia en estado " + current);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        AppUser actor = userRepository.getReferenceById(user.userId());

        incident.setStatus(IncidentStatus.PAUSED);
        incident.setPauseReason(request.reason().strip());

        incidentHistoryService.record(incident, actor, IncidentEventType.STATUS_CHANGED,
                current.name(), IncidentStatus.PAUSED.name(), now);

        incidentRepository.save(incident);
        return IncidentResponse.from(incident);
    }

    // ---------- CU-EXE-05: resume ----------

    @Transactional
    public IncidentResponse resume(Long incidentId, AuthUser user) {
        Incident incident = incidentService.getOwnedByAccountOr404(incidentId, user);
        incidentAccessPolicy.ensureCanActOn(incident, user);

        IncidentStatus current = incident.getStatus();
        if (current != IncidentStatus.PAUSED) {
            throw new ConflictException("No se puede reanudar una incidencia en estado " + current);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        AppUser actor = userRepository.getReferenceById(user.userId());

        incident.setStatus(IncidentStatus.IN_PROGRESS);

        incidentHistoryService.record(incident, actor, IncidentEventType.STATUS_CHANGED,
                current.name(), IncidentStatus.IN_PROGRESS.name(), now);

        incidentRepository.save(incident);
        return IncidentResponse.from(incident);
    }
}