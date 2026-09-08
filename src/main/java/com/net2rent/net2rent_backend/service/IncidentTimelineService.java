package com.net2rent.net2rent_backend.service;

import com.net2rent.net2rent_backend.dto.response.TimelineItemResponse;
import com.net2rent.net2rent_backend.model.AppUser;
import com.net2rent.net2rent_backend.model.IncidentHistory;
import com.net2rent.net2rent_backend.repository.UserRepository;
import com.net2rent.net2rent_backend.repository.IncidentCommentRepository;
import com.net2rent.net2rent_backend.repository.IncidentHistoryRepository;
import com.net2rent.net2rent_backend.security.AuthUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class IncidentTimelineService {
    private final IncidentService incidentService;
    private final IncidentHistoryRepository incidentHistoryRepository;
    private final IncidentCommentRepository incidentCommentRepository;
    private final UserRepository userRepository;

    public IncidentTimelineService(IncidentService incidentService,
                                   IncidentHistoryRepository incidentHistoryRepository,
                                   IncidentCommentRepository incidentCommentRepository,
                                   UserRepository userRepository) {
        this.incidentService = incidentService;
        this.incidentHistoryRepository = incidentHistoryRepository;
        this.incidentCommentRepository = incidentCommentRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<TimelineItemResponse> getTimeline(Long incidentId, AuthUser user) {
        incidentService.getOwnedByAccountOr404(incidentId, user);

        List<IncidentHistory> historyEvents =
                incidentHistoryRepository.findByIncident_IdOrderByCreatedAtAsc(incidentId);

        Map<Long, String> operatorNames = resolveOperatorNames(historyEvents);

        Stream<TimelineItemResponse> events = historyEvents.stream()
                .map(h -> TimelineItemResponse.fromEvent(h, operatorNames));

        Stream<TimelineItemResponse> comments = incidentCommentRepository
                .findByIncident_IdOrderByCreatedAtAsc(incidentId).stream()
                .map(TimelineItemResponse::fromComment);

        return Stream.concat(events, comments)
                .sorted(Comparator.comparing(TimelineItemResponse::at))
                .toList();
    }

    private Map<Long, String> resolveOperatorNames(List<IncidentHistory> events) {
        Set<Long> ids = new HashSet<>();
        for (IncidentHistory h : events) {
            if (TimelineItemResponse.referencesOperator(h.getEventType())) {
                addIfId(ids, h.getPreviousValue());
                addIfId(ids, h.getNewValue());
            }
        }
        if (ids.isEmpty()) return Map.of();
        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(
                        AppUser::getId,
                        u -> u.getFirstName() + " " + u.getLastName()));
    }

    private void addIfId(Set<Long> ids, String value) {
        if (value == null) return;
        try {
            ids.add(Long.valueOf(value.trim()));
        } catch (NumberFormatException ignored) { /* no es un id, lo saltamos */ }
    }
}