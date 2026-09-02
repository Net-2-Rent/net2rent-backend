package com.net2rent.net2rent_backend.repository;

import com.net2rent.net2rent_backend.model.IncidentHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentHistoryRepository extends JpaRepository<IncidentHistory, Long> {
}