package com.net2rent.net2rent_backend.repository;

import com.net2rent.net2rent_backend.model.LodgingCounter;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface LodgingCounterRepository extends JpaRepository<LodgingCounter, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from LodgingCounter c")
    Optional<LodgingCounter> findForUpdate();

}
