package com.net2rent.net2rent_backend.service;

import com.net2rent.net2rent_backend.dto.request.CreateCommentRequest;
import com.net2rent.net2rent_backend.dto.response.TimelineItemResponse;
import com.net2rent.net2rent_backend.exception.ConflictException;
import com.net2rent.net2rent_backend.model.Incident;
import com.net2rent.net2rent_backend.model.IncidentComment;
import com.net2rent.net2rent_backend.model.enums.IncidentStatus;
import com.net2rent.net2rent_backend.repository.IncidentCommentRepository;
import com.net2rent.net2rent_backend.repository.UserRepository;
import com.net2rent.net2rent_backend.security.AuthUser;
import com.net2rent.net2rent_backend.security.IncidentAccessPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class IncidentCommentService {

    private final IncidentService incidentService;
    private final IncidentAccessPolicy incidentAccessPolicy;
    private final IncidentCommentRepository incidentCommentRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public IncidentCommentService(IncidentService incidentService,
                                  IncidentAccessPolicy incidentAccessPolicy,
                                  IncidentCommentRepository incidentCommentRepository,
                                  UserRepository userRepository,
                                  Clock clock) {
        this.incidentService = incidentService;
        this.incidentAccessPolicy = incidentAccessPolicy;
        this.incidentCommentRepository = incidentCommentRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional
    public TimelineItemResponse addComment(Long incidentId, CreateCommentRequest request, AuthUser user) {
        Incident incident = incidentService.getOwnedByAccountOr404(incidentId, user);

        incidentAccessPolicy.ensureCanActOn(incident, user);

        if (incident.getStatus() == IncidentStatus.CLOSED
                || incident.getStatus() == IncidentStatus.REJECTED) {
            throw new ConflictException("La incidencia está cerrada");
        }

        IncidentComment comment = IncidentComment.builder()
                .incident(incident)
                .author(userRepository.getReferenceById(user.userId()))
                .text(request.text())
                .createdAt(LocalDateTime.now(clock))
                .build();

        IncidentComment saved = incidentCommentRepository.save(comment);
        return TimelineItemResponse.fromComment(saved);
    }
}