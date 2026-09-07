package com.net2rent.net2rent_backend.service;

import com.net2rent.net2rent_backend.dto.request.CreateChecklistItemRequest;
import com.net2rent.net2rent_backend.dto.request.UpdateChecklistItemRequest;
import com.net2rent.net2rent_backend.dto.response.ChecklistItemResponse;
import com.net2rent.net2rent_backend.exception.ConflictException;
import com.net2rent.net2rent_backend.exception.NotFoundException;
import com.net2rent.net2rent_backend.model.Incident;
import com.net2rent.net2rent_backend.model.IncidentCheckListItem;
import com.net2rent.net2rent_backend.model.enums.IncidentStatus;
import com.net2rent.net2rent_backend.repository.IncidentCheckListItemRepository;
import com.net2rent.net2rent_backend.repository.UserRepository;
import com.net2rent.net2rent_backend.security.AuthUser;
import com.net2rent.net2rent_backend.security.IncidentAccessPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class IncidentChecklistService {

    private final IncidentService incidentService;
    private final IncidentAccessPolicy incidentAccessPolicy;
    private final IncidentCheckListItemRepository checklistItemRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public IncidentChecklistService(IncidentService incidentService,
                                    IncidentAccessPolicy incidentAccessPolicy,
                                    IncidentCheckListItemRepository checklistItemRepository,
                                    UserRepository userRepository,
                                    Clock clock) {
        this.incidentService = incidentService;
        this.incidentAccessPolicy = incidentAccessPolicy;
        this.checklistItemRepository = checklistItemRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ChecklistItemResponse> list(Long incidentId, AuthUser user) {
        Incident incident = incidentService.getOwnedByAccountOr404(incidentId, user);
        return checklistItemRepository.findByIncident_IdOrderByIdAsc(incident.getId())
                .stream()
                .map(ChecklistItemResponse::from)
                .toList();
    }

    @Transactional
    public ChecklistItemResponse addItem(Long incidentId, CreateChecklistItemRequest request, AuthUser user) {
        Incident incident = incidentService.getOwnedByAccountOr404(incidentId, user);
        incidentAccessPolicy.ensureCanActOn(incident, user);
        ensureNotTerminal(incident);

        IncidentCheckListItem item = IncidentCheckListItem.builder()
                .incident(incident)
                .text(request.text())
                .done(false)
                .build();

        return ChecklistItemResponse.from(checklistItemRepository.save(item));
    }

    @Transactional
    public ChecklistItemResponse setDone(Long incidentId, Long itemId,
                                         UpdateChecklistItemRequest request, AuthUser user) {
        Incident incident = incidentService.getOwnedByAccountOr404(incidentId, user);
        incidentAccessPolicy.ensureCanActOn(incident, user);
        ensureNotTerminal(incident);

        IncidentCheckListItem item = checklistItemRepository
                .findByIdAndIncident_Id(itemId, incident.getId())
                .orElseThrow(() -> new NotFoundException("Item de checklist no encontrado"));

        item.setDone(request.done());
        item.setCheckedBy(userRepository.getReferenceById(user.userId()));
        item.setCheckedAt(LocalDateTime.now(clock));

        return ChecklistItemResponse.from(checklistItemRepository.save(item));
    }

    @Transactional
    public void deleteItem(Long incidentId, Long itemId, AuthUser user) {
        Incident incident = incidentService.getOwnedByAccountOr404(incidentId, user);
        incidentAccessPolicy.ensureCanActOn(incident, user);
        ensureNotTerminal(incident);

        IncidentCheckListItem item = checklistItemRepository
                .findByIdAndIncident_Id(itemId, incident.getId())
                .orElseThrow(() -> new NotFoundException("Item de checklist no encontrado"));

        checklistItemRepository.delete(item);
    }

    private void ensureNotTerminal(Incident incident) {
        if (incident.getStatus() == IncidentStatus.CLOSED
                || incident.getStatus() == IncidentStatus.REJECTED) {
            throw new ConflictException("La incidencia está cerrada");
        }
    }
}