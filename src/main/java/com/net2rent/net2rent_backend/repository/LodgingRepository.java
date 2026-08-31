package com.net2rent.net2rent_backend.repository;

import com.net2rent.net2rent_backend.model.Lodging;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LodgingRepository extends JpaRepository<Lodging, Long> {

    List<Lodging> findByAccount_Id(Long accountId);

    Optional<Lodging> findByIdAndAccount_Id(Long id, Long accountId);
}