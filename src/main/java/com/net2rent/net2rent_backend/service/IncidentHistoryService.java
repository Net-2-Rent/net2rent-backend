package com.net2rent.net2rent_backend.service;

import com.net2rent.net2rent_backend.model.AppUser;
import com.net2rent.net2rent_backend.model.Incident;
import com.net2rent.net2rent_backend.model.IncidentHistory;
import com.net2rent.net2rent_backend.model.enums.IncidentEventType;
import com.net2rent.net2rent_backend.repository.IncidentHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class IncidentHistoryService {
    private final IncidentHistoryRepository incidentHistoryRepository;

    public IncidentHistoryService(IncidentHistoryRepository incidentHistoryRepository){
        this.incidentHistoryRepository = incidentHistoryRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Incident incident, AppUser actor, IncidentEventType eventType,
                       String previousValue, String newValue, LocalDateTime occuredAt) {

        IncidentHistory event = IncidentHistory.builder()
                .incident(incident)
                .actor(actor)
                .eventType(eventType.name())
                .previousValue(previousValue)
                .newValue(newValue)
                .createdAt(occuredAt)
                .build();

        incidentHistoryRepository.save(event);
    }
}