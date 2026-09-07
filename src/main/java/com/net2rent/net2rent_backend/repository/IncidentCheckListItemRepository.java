package com.net2rent.net2rent_backend.repository;

import com.net2rent.net2rent_backend.model.IncidentCheckListItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IncidentCheckListItemRepository extends JpaRepository<IncidentCheckListItem, Long> {

    List<IncidentCheckListItem> findByIncident_IdOrderByIdAsc(Long incidentId);

    Optional<IncidentCheckListItem> findByIdAndIncident_Id(Long id, Long incidentId);
}