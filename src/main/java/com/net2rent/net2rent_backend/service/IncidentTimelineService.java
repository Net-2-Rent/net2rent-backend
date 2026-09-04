package com.net2rent.net2rent_backend.service;

import com.net2rent.net2rent_backend.dto.response.TimelineItemResponse;
import com.net2rent.net2rent_backend.repository.IncidentCommentRepository;
import com.net2rent.net2rent_backend.repository.IncidentHistoryRepository;
import com.net2rent.net2rent_backend.security.AuthUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
public class IncidentTimelineService {
    private final IncidentService incidentService;
    private final IncidentHistoryRepository incidentHistoryRepository;
    private final IncidentCommentRepository incidentCommentRepository;

    public IncidentTimelineService(IncidentService incidentService,
                                   IncidentHistoryRepository incidentHistoryRepository,
                                   IncidentCommentRepository incidentCommentRepository) {
        this.incidentService = incidentService;
        this.incidentHistoryRepository = incidentHistoryRepository;
        this.incidentCommentRepository = incidentCommentRepository;
    }

    @Transactional(readOnly = true)
    public List<TimelineItemResponse> getTimeline(Long incidentId, AuthUser user) {
        incidentService.getOwnedByAccountOr404(incidentId, user);

        Stream<TimelineItemResponse> events = incidentHistoryRepository
                .findByIncident_IdOrderByCreatedAtAsc(incidentId).stream()
                .map(TimelineItemResponse::fromEvent);

        Stream<TimelineItemResponse> comments = incidentCommentRepository
                .findByIncident_IdOrderByCreatedAtAsc(incidentId).stream()
                .map(TimelineItemResponse::fromComment);

        return Stream.concat(events, comments)
                .sorted(Comparator.comparing(TimelineItemResponse::at))
                .toList();
    }
}