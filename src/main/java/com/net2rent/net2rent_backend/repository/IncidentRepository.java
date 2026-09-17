package com.net2rent.net2rent_backend.repository;

import com.net2rent.net2rent_backend.model.Incident;
import com.net2rent.net2rent_backend.model.enums.IncidentStatus;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface IncidentRepository
        extends JpaRepository<Incident, Long>, JpaSpecificationExecutor<Incident> {

    Optional<Incident> findByIdAndAccount_Id(Long id, Long accountId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Incident i where i.id = :id and i.account.id = :accountId")
    Optional<Incident> findByIdAndAccount_IdForUpdate(@Param("id") Long id,
                                                      @Param("accountId") Long accountId);

    List<Incident> findByLodging_IdOrderByOpenedAtDesc(Long lodgingId);

    Optional<Incident> findByIdAndLodging_Id(Long id, Long lodgingId);

    long countByAssignee_IdAndStatusIn(Long assigneeId, Collection<IncidentStatus> statuses);

    @Query("""
        select distinct i from Incident i
        left join fetch i.images
        where i.id = :id and i.lodging.id = :lodgingId
        """)
    Optional<Incident> findByIdAndLodging_IdWithImages(@Param("id") Long id, @Param("lodgingId") Long lodgingId);
}