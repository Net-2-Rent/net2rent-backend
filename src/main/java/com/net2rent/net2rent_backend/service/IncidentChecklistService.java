package com.net2rent.net2rent_backend.service;

import com.net2rent.net2rent_backend.dto.request.CreateChecklistItemRequest;
import com.net2rent.net2rent_backend.dto.request.ReorderChecklistRequest;
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
import java.util.*;
import java.util.stream.Collectors;

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
    public long countPending(Long incidentId) {
        return checklistItemRepository.countByIncident_IdAndDoneFalse(incidentId);
    }

    @Transactional(readOnly = true)
    public List<ChecklistItemResponse> list(Long incidentId, AuthUser user) {
        Incident incident = incidentService.getOwnedByAccountOr404(incidentId, user);
        return checklistItemRepository.findByIncident_IdOrderByPositionAscIdAsc(incident.getId())
                .stream()
                .map(ChecklistItemResponse::from)
                .toList();
    }

    @Transactional
    public List<ChecklistItemResponse> addItem(Long incidentId, CreateChecklistItemRequest request, AuthUser user) {
        Incident incident = incidentService.getOwnedByAccountOr404(incidentId, user);
        incidentAccessPolicy.ensureCanActOn(incident, user);
        ensureNotTerminal(incident);

        IncidentCheckListItem item = IncidentCheckListItem.builder()
                .incident(incident)
                .text(request.text())
                .done(false)
                .build();
        checklistItemRepository.save(item);

        // Reconstruir orden: [pendientes existentes] + [nuevo] + [hechas]
        List<IncidentCheckListItem> all = checklistItemRepository
                .findByIncident_IdOrderByPositionAscIdAsc(incident.getId());

        List<Long> pendingIds = all.stream()
                .filter(i -> !i.isDone() && !i.getId().equals(item.getId()))
                .map(IncidentCheckListItem::getId)
                .toList();
        List<Long> doneIds = all.stream()
                .filter(IncidentCheckListItem::isDone)
                .map(IncidentCheckListItem::getId)
                .toList();

        List<Long> orderedIds = new ArrayList<>(pendingIds);
        orderedIds.add(item.getId());
        orderedIds.addAll(doneIds);

        return persistOrder(incident, orderedIds);
    }

    @Transactional
    public List<ChecklistItemResponse> setDone(Long incidentId, Long itemId,
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
        checklistItemRepository.save(item);

        // Reconstruir orden completo
        List<IncidentCheckListItem> all = checklistItemRepository
                .findByIncident_IdOrderByPositionAscIdAsc(incident.getId());

        List<Long> otherIds = all.stream()
                .filter(i -> !i.getId().equals(itemId))
                .map(IncidentCheckListItem::getId)
                .toList();

        List<Long> orderedIds = new ArrayList<>();
        if (request.done()) {
            // Hecha → al final
            orderedIds.addAll(otherIds);
            orderedIds.add(itemId);
        } else {
            // Pendiente → al principio
            orderedIds.add(itemId);
            orderedIds.addAll(otherIds);
        }

        return persistOrder(incident, orderedIds);
    }

    @Transactional
    public List<ChecklistItemResponse> deleteItem(Long incidentId, Long itemId, AuthUser user) {
        Incident incident = incidentService.getOwnedByAccountOr404(incidentId, user);
        incidentAccessPolicy.ensureCanActOn(incident, user);
        ensureNotTerminal(incident);

        IncidentCheckListItem item = checklistItemRepository
                .findByIdAndIncident_Id(itemId, incident.getId())
                .orElseThrow(() -> new NotFoundException("Item de checklist no encontrado"));

        checklistItemRepository.delete(item);

        // Reindexar los que quedan
        List<IncidentCheckListItem> remaining = checklistItemRepository
                .findByIncident_IdOrderByPositionAscIdAsc(incident.getId());
        List<Long> remainingIds = remaining.stream()
                .map(IncidentCheckListItem::getId)
                .toList();

        return persistOrder(incident, remainingIds);
    }

    @Transactional
    public List<ChecklistItemResponse> reorder(Long incidentId,
            ReorderChecklistRequest request,
            AuthUser user) {
        Incident incident = incidentService.getOwnedByAccountOr404(incidentId, user);
        incidentAccessPolicy.ensureCanActOn(incident, user);
        ensureNotTerminal(incident);

        return persistOrder(incident, request.orderedIds());
    }

    // ---------- Helper ----------

    private List<ChecklistItemResponse> persistOrder(Incident incident, List<Long> orderedIds) {
        List<IncidentCheckListItem> all = checklistItemRepository
                .findByIncident_IdOrderByPositionAscIdAsc(incident.getId());

        Set<Long> existingIds = all.stream()
                .map(IncidentCheckListItem::getId)
                .collect(Collectors.toSet());

        // Validar: mismos ids (sin duplicados) y sin faltantes/extraños
        boolean hasDuplicates = new HashSet<>(orderedIds).size() != orderedIds.size();
        boolean sameSet = new HashSet<>(orderedIds).equals(existingIds);

        if (hasDuplicates || !sameSet) {
            throw new ConflictException(
                    "La lista de ids no coincide con los items de la incidencia");
        }

        Map<Long, IncidentCheckListItem> byId = all.stream()
                .collect(Collectors.toMap(IncidentCheckListItem::getId, i -> i));

        int pos = 0;
        for (Long id : orderedIds) {
            byId.get(id).setPosition(pos++);
        }

        checklistItemRepository.saveAll(all);

        return orderedIds.stream()
                .map(id -> byId.get(id))
                .map(ChecklistItemResponse::from)
                .toList();
    }

    private void ensureNotTerminal(Incident incident) {
        if (incident.getStatus() == IncidentStatus.CLOSED
                || incident.getStatus() == IncidentStatus.REJECTED) {
            throw new ConflictException("La incidencia está cerrada");
        }
    }
}