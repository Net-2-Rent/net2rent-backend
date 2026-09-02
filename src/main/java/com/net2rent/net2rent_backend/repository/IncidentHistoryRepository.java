package com.net2rent.net2rent_backend.repository;

<<<<<<< HEAD
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.net2rent.net2rent_backend.model.IncidentHistory;

public interface IncidentHistoryRepository extends JpaRepository<IncidentHistory, Long> {

    List<IncidentHistory> findByIncident_IdOrderByCreatedAtAsc(Long incidentId);

}
=======
import com.net2rent.net2rent_backend.model.IncidentHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentHistoryRepository extends JpaRepository<IncidentHistory, Long> {
}
>>>>>>> origin/dev
