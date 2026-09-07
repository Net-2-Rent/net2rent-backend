package com.net2rent.net2rent_backend.repository;

import com.net2rent.net2rent_backend.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account,Long> {

}
