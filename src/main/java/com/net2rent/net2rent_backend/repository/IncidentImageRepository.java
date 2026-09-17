package com.net2rent.net2rent_backend.repository;

import com.net2rent.net2rent_backend.model.IncidentImage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface IncidentImageRepository extends JpaRepository<IncidentImage, Long> {
    Optional<IncidentImage> findByIdAndIncident_Id(Long id, Long incidentId);
}