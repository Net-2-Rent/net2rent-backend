package com.net2rent.net2rent_backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.net2rent.net2rent_backend.model.IncidentTimeEntry;

public interface IncidentTimeEntryRepository extends JpaRepository<IncidentTimeEntry, Long> {

    List<IncidentTimeEntry> findByIncident_IdOrderByCreatedAtAscIdAsc(Long incidentId);
    Optional<IncidentTimeEntry> findByIdAndIncident_Id(Long id, Long indicentId);

}
