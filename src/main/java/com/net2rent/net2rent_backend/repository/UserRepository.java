package com.net2rent.net2rent_backend.repository;

import java.util.*;
import com.net2rent.net2rent_backend.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmail(String email);

}
