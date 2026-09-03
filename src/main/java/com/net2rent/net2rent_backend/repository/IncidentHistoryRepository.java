package com.net2rent.net2rent_backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.net2rent.net2rent_backend.model.IncidentHistory;

public interface IncidentHistoryRepository extends JpaRepository<IncidentHistory, Long> {

    List<IncidentHistory> findByIncident_IdOrderByCreatedAtAsc(Long incidentId);

}
