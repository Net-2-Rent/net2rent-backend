package com.net2rent.net2rent_backend.service;

import com.net2rent.net2rent_backend.dto.ClassifyIncidentRequest;
import com.net2rent.net2rent_backend.dto.CorrectIncidentTextRequest;
import com.net2rent.net2rent_backend.dto.IncidentResponse;
import com.net2rent.net2rent_backend.exception.NotFoundException;
import com.net2rent.net2rent_backend.model.AppUser;
import com.net2rent.net2rent_backend.model.Incident;
import com.net2rent.net2rent_backend.model.IncidentHistory;
import com.net2rent.net2rent_backend.model.enums.IncidentCategory;
import com.net2rent.net2rent_backend.model.enums.IncidentPriority;
import com.net2rent.net2rent_backend.model.enums.UserRole;
import com.net2rent.net2rent_backend.repository.IncidentHistoryRepository;
import com.net2rent.net2rent_backend.repository.IncidentRepository;
import com.net2rent.net2rent_backend.repository.UserRepository;
import com.net2rent.net2rent_backend.security.AuthUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class IncidentService {

    private static final String CATEGORY_CHANGED = "CATEGORY_CHANGED";
    private static final String PRIORITY_CHANGED = "PRIORITY_CHANGED";
    private static final String TITLE_CHANGED = "TITLE_CHANGED";
    private static final String DESCRIPTION_CHANGED = "DESCRIPTION_CHANGED";

    private static final int TITLE_MAX_FROM_DESCRIPTION = 80;

    private final IncidentRepository incidentRepository;
    private final IncidentHistoryRepository incidentHistoryRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public IncidentService(IncidentRepository incidentRepository, IncidentHistoryRepository incidentHistoryRepository,
            UserRepository userRepository, Clock clock) {
        this.incidentRepository = incidentRepository;
        this.incidentHistoryRepository = incidentHistoryRepository;
        this.userRepository = userRepository;
        this.clock = clock;
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

    @Transactional
    public IncidentResponse classify(Long incidentId, ClassifyIncidentRequest request, AuthUser user) {
        Incident incident = getOwnedByAccountOr404(incidentId, user);
        AppUser actor = userRepository.getReferenceById(user.userId());

        IncidentCategory oldCategory = incident.getCategory();
        if (oldCategory != request.category()) {
            incident.setCategory(request.category());
            recordChange(incident, actor, CATEGORY_CHANGED, nameOrNull(oldCategory), request.category().name());
        }

        IncidentPriority oldPriority = incident.getPriority();
        if (oldPriority != request.priority()) {
            incident.setPriority(request.priority());
            recordChange(incident, actor, PRIORITY_CHANGED, nameOrNull(oldPriority), request.priority().name());
        }

        incidentRepository.save(incident);
        return IncidentResponse.from(incident);
    }

    @Transactional
    public IncidentResponse markUrgent(Long incidentId, AuthUser user) {
        Incident incident = getOwnedByAccountOr404(incidentId, user);
        AppUser actor = userRepository.getReferenceById(user.userId());

        IncidentPriority oldPriority = incident.getPriority();
        if (oldPriority != IncidentPriority.URGENT) {
            incident.setPriority(IncidentPriority.URGENT);
            recordChange(incident, actor, PRIORITY_CHANGED, nameOrNull(oldPriority), IncidentPriority.URGENT.name());
            incidentRepository.save(incident);
        }

        return IncidentResponse.from(incident);
    }

    @Transactional
    public IncidentResponse correctText(Long incidentId, CorrectIncidentTextRequest request, AuthUser user) {
        Incident incident = getOwnedByAccountOr404(incidentId, user);
        AppUser actor = userRepository.getReferenceById(user.userId());

        String oldDescription = incident.getDescription();
        if (!request.description().equals(oldDescription)) {
            incident.setDescription(request.description());
            recordChange(incident, actor, DESCRIPTION_CHANGED, oldDescription, request.description());
        }

        String newTitle = resolveTitle(request.title(), request.description());
        String oldTitle = incident.getTitle();
        if (!newTitle.equals(oldTitle)) {
            incident.setTitle(newTitle);
            recordChange(incident, actor, TITLE_CHANGED, oldTitle, newTitle);
        }

        incidentRepository.save(incident);
        return IncidentResponse.from(incident);
    }

    private String resolveTitle(String title, String description) {
        if (title != null && !title.isBlank()) {
            return title.trim();
        }
        String base = description.trim();
        return base.length() <= TITLE_MAX_FROM_DESCRIPTION
                ? base
                : base.substring(0, TITLE_MAX_FROM_DESCRIPTION).trim();
    }

    private void recordChange(Incident incident, AppUser actor, String eventType,
            String previousValue, String newValue) {
        IncidentHistory event = IncidentHistory.builder()
                .incident(incident)
                .actor(actor)
                .eventType(eventType)
                .previousValue(previousValue)
                .newValue(newValue)
                .createdAt(LocalDateTime.now(clock))
                .build();
        incidentHistoryRepository.save(event);
    }

    private static String nameOrNull(Enum<?> value) {
        return value == null ? null : value.name();
    }

}