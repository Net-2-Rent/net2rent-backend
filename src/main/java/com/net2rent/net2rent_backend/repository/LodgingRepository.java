package com.net2rent.net2rent_backend.repository;

import java.util.*;
import com.net2rent.net2rent_backend.model.Lodging;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LodgingRepository extends JpaRepository<Lodging, Long> {
    Optional<Lodging> findByRef(String ref);

}
