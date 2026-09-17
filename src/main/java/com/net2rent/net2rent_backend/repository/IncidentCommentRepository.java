package com.net2rent.net2rent_backend.repository;

import com.net2rent.net2rent_backend.model.IncidentComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncidentCommentRepository extends JpaRepository<IncidentComment, Long> {
    List<IncidentComment> findByIncident_IdOrderByCreatedAtAsc(Long incidentId);
}