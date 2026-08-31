package com.net2rent.net2rent_backend.repository;

import com.net2rent.net2rent_backend.model.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    Optional<Incident> findByIdAndAccount_Id(Long id, Long accountId);

    List<Incident> findByAccount_Id(Long accountId);

    @Query("""
            select i from Incident i
            where i.account.id = :accountId
                and (i.assignee.id = :userId or i.assignee is null)
            """)
    List<Incident> findVisibleToOperator(@Param("accountId") Long accountId,
                                         @Param("userId") Long userId);
}