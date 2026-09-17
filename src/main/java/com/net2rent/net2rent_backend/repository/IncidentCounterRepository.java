package com.net2rent.net2rent_backend.repository;

import com.net2rent.net2rent_backend.model.IncidentCounter;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface IncidentCounterRepository extends JpaRepository<IncidentCounter, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select c from IncidentCounter c
            where c.account.id = :accountId and c.year = :year
            """)
    Optional<IncidentCounter> findForUpdate(@Param("accountId") Long accountId,
                                            @Param("year") int year);
}