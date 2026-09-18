package com.net2rent.net2rent_backend.service;

import com.net2rent.net2rent_backend.dto.request.AssignOperatorRequest;
import com.net2rent.net2rent_backend.dto.request.RejectIncidentRequest;
import com.net2rent.net2rent_backend.dto.response.IncidentResponse;
import com.net2rent.net2rent_backend.dto.request.PauseIncidentRequest;
import com.net2rent.net2rent_backend.dto.request.ResolveIncidentRequest;
import com.net2rent.net2rent_backend.exception.ConflictException;
import com.net2rent.net2rent_backend.exception.NotFoundException;
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
    private final IncidentChecklistService incidentChecklistService;

    public IncidentExecutionService(IncidentService incidentService,
                                    IncidentAccessPolicy incidentAccessPolicy,
                                    IncidentRepository incidentRepository,
                                    IncidentHistoryService incidentHistoryService,
                                    UserRepository userRepository,
                                    Clock clock,
                                    IncidentChecklistService incidentChecklistService) {
        this.incidentService = incidentService;
        this.incidentAccessPolicy = incidentAccessPolicy;
        this.incidentRepository = incidentRepository;
        this.incidentHistoryService = incidentHistoryService;
        this.userRepository = userRepository;
        this.clock = clock;
        this.incidentChecklistService = incidentChecklistService;
    }

    @Transactional
    public IncidentResponse start(Long incidentId, AuthUser user) {
        Incident incident = incidentService.getOwnedByAccountOr404(incidentId, user);
        incidentAccessPolicy.ensureCanActOn(incident, user);

        IncidentStatus current = incident.getStatus();
        if (!IncidentTransitions.STARTABLE_FROM.contains(current)) {
            throw new ConflictException("No se puede comenzar una incidencia en estado " + current);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        AppUser actor = userRepository.getReferenceById(user.userId());

        incident.setStatus(IncidentStatus.IN_PROGRESS);
        incident.setStartedAt(now);

        incidentHistoryService.record(incident, actor, IncidentEventType.STATUS_CHANGED,
                current.name(), IncidentStatus.IN_PROGRESS.name(), null, now);

        incidentRepository.save(incident);
        return IncidentResponse.from(incident);
    }

    @Transactional
    public IncidentResponse pause(Long incidentId, PauseIncidentRequest request, AuthUser user) {
        Incident incident = incidentService.getOwnedByAccountOr404(incidentId, user);
        incidentAccessPolicy.ensureCanActOn(incident, user);

        IncidentStatus current = incident.getStatus();
        if (!IncidentTransitions.PAUSABLE_FROM.contains(current)) {
            throw new ConflictException("No se puede pausar una incidencia en estado " + current);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        AppUser actor = userRepository.getReferenceById(user.userId());

        incident.setStatus(IncidentStatus.PAUSED);
        incident.setPauseReason(request.reason().strip());

        incidentHistoryService.record(incident, actor, IncidentEventType.STATUS_CHANGED,
                current.name(), IncidentStatus.PAUSED.name(), request.reason().strip(), now);

        incidentRepository.save(incident);
        return IncidentResponse.from(incident);
    }

    @Transactional
    public IncidentResponse resume(Long incidentId, AuthUser user) {
        Incident incident = incidentService.getOwnedByAccountOr404(incidentId, user);
        incidentAccessPolicy.ensureCanActOn(incident, user);

        IncidentStatus current = incident.getStatus();
        if (!IncidentTransitions.RESUMABLE_FROM.contains(current)) {
            throw new ConflictException("No se puede reanudar una incidencia en estado " + current);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        AppUser actor = userRepository.getReferenceById(user.userId());

        incident.setStatus(IncidentStatus.IN_PROGRESS);

        incidentHistoryService.record(incident, actor, IncidentEventType.STATUS_CHANGED,
                current.name(), IncidentStatus.IN_PROGRESS.name(), null, now);

        incidentRepository.save(incident);
        return IncidentResponse.from(incident);
    }

    @Transactional
    public IncidentResponse resolve(Long incidentId, ResolveIncidentRequest request, AuthUser user) {
        Incident incident = incidentService.getOwnedByAccountOr404(incidentId, user);
        incidentAccessPolicy.ensureCanActOn(incident, user);

        IncidentStatus current = incident.getStatus();
        if (!IncidentTransitions.RESOLVABLE_FROM.contains(current)) {
            throw new ConflictException("No se puede resolver una incidencia en estado " + current);
        }

        if (incident.getCategory() == null) {
            throw new ConflictException("La incidencia necesita una categoría");
        }

        long pending = incidentChecklistService.countPending(incidentId);
        if (pending > 0) {
            throw new ConflictException("Quedan " + pending + " tareas del checklist sin completar");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        AppUser actor = userRepository.getReferenceById(user.userId());

        incident.setStatus(IncidentStatus.RESOLVED);
        incident.setResolvedAt(now);
        incident.setMinutesSpent(request.minutes());
        incident.setResolutionNote(request.note().strip());

        incidentHistoryService.record(incident, actor, IncidentEventType.STATUS_CHANGED,
                current.name(), IncidentStatus.RESOLVED.name(), request.note().strip(), now);

        incidentHistoryService.record(incident, actor, IncidentEventType.TIME_LOGGED,
                null, String.valueOf(request.minutes()), null, now);

        incidentRepository.save(incident);
        return IncidentResponse.from(incident);
    }

    @Transactional
    public IncidentResponse reject(Long incidentId, RejectIncidentRequest request, AuthUser user) {
        Incident incident = incidentService.getOwnedByAccountOr404(incidentId, user);
        incidentAccessPolicy.ensureCanActOn(incident, user);

        IncidentStatus current = incident.getStatus();
        incidentAccessPolicy.ensureWorkEditable(incident);
        if (current == IncidentStatus.RESOLVED) {
            throw new ConflictException("No se puede rechazar una incidencia ya resuelta");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        AppUser actor = userRepository.getReferenceById(user.userId());

        incident.setStatus(IncidentStatus.REJECTED);
        incident.setRejectionReason(request.reason().strip());
        incident.setClosedAt(now);

        incidentHistoryService.record(incident, actor, IncidentEventType.STATUS_CHANGED,
                current.name(), IncidentStatus.REJECTED.name(), null, now);

        incidentRepository.save(incident);
        return IncidentResponse.from(incident);
    }

    @Transactional
    public IncidentResponse assignOperator(Long incidentId, AssignOperatorRequest request, AuthUser user) {
        Incident incident = incidentService.getOwnedByAccountOr404(incidentId, user);
        incidentAccessPolicy.ensureCanActOn(incident, user);
        incidentAccessPolicy.ensureWorkEditable(incident);

        IncidentStatus status = incident.getStatus();
        if (status == IncidentStatus.RESOLVED) {
            throw new ConflictException("No se puede asignar una incidencia resuelta");
        }

        AppUser newOperator = incidentService.validateOperator(request.operatorId(), user.accountId());
        AppUser current = incident.getAssignee();
        LocalDateTime now = LocalDateTime.now(clock);
        AppUser actor = userRepository.getReferenceById(user.userId());

        if (current == null) {
            incident.setAssignee(newOperator);
            incident.setAssignedAt(now);
            incident.setStatus(IncidentStatus.ASSIGNED);
            incidentHistoryService.record(incident, actor, IncidentEventType.ASSIGNED, null,
                    newOperator.getId().toString(), null, now);
        } else {
            if (current.getId().equals(newOperator.getId())) {
                throw new ConflictException("El operario ya está asignado a esta incidencia");
            }
            String reason = request.reason() == null ? "" : request.reason().strip();
            if (reason.isEmpty()) {
                throw new ConflictException("El motivo de la reasignación es obligatorio");
            }

            incident.setAssignee(newOperator);
            incident.setAssignedAt(now);
            incidentHistoryService.record(incident, actor, IncidentEventType.REASSIGNED, current.getId().toString(),
                    newOperator.getId().toString(), reason, now);

            if (status == IncidentStatus.IN_PROGRESS || status == IncidentStatus.PAUSED) {
                incident.setStatus(IncidentStatus.ASSIGNED);
                incidentHistoryService.record(incident, actor, IncidentEventType.STATUS_CHANGED, status.name(),
                        IncidentStatus.ASSIGNED.name(), null, now);
            }
        }

        incidentRepository.save(incident);
        return IncidentResponse.from(incident);
    }

    @Transactional
    public IncidentResponse close(Long incidentId, AuthUser user) {
        Incident incident = incidentService.getOwnedByAccountOr404(incidentId, user); // 404 por cuenta (ADR-001)
        incidentAccessPolicy.ensureCanActOn(incident, user);

        IncidentStatus current = incident.getStatus();
        if (!IncidentTransitions.CLOSABLE_FROM.contains(current)) {
            throw new ConflictException("Solo se puede cerrar una incidencia resuelta");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        AppUser actor = userRepository.getReferenceById(user.userId());

        incident.setStatus(IncidentStatus.CLOSED);
        incident.setClosedAt(now);

        incidentHistoryService.record(incident, actor, IncidentEventType.STATUS_CHANGED,
                current.name(), IncidentStatus.CLOSED.name(), null, now);

        incidentRepository.save(incident);
        return IncidentResponse.from(incident);
    }
    
    @Transactional
    public IncidentResponse claim(Long incidentId, AuthUser user) {
        Incident incident = incidentRepository
                .findByIdAndAccount_IdForUpdate(incidentId, user.accountId())
                .orElseThrow(() -> new NotFoundException("Incidencia no encontrada"));

        if (incident.getAssignee() != null) {
            throw new ConflictException("Esta incidencia ya ha sido asignada");
        }
        if (!IncidentTransitions.CLAIMABLE_FROM.contains(incident.getStatus())) {
            throw new ConflictException("La incidencia no está disponible en el pool");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        AppUser operator = userRepository.getReferenceById(user.userId());

        incident.setAssignee(operator);
        incident.setStatus(IncidentStatus.ASSIGNED);
        incident.setAssignedAt(now);

        incidentHistoryService.record(incident, operator, IncidentEventType.ASSIGNED,
                null, operator.getId().toString(), null, now);

        incidentRepository.save(incident);
        return IncidentResponse.from(incident);
    }
}