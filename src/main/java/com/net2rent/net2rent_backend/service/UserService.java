package com.net2rent.net2rent_backend.service;

import com.net2rent.net2rent_backend.dto.OperatorResponse;
import com.net2rent.net2rent_backend.model.enums.UserRole;
import com.net2rent.net2rent_backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<OperatorResponse> listAssignableOperators(Long accountId) {
        return userRepository
                .findByAccount_IdAndRoleAndActiveTrueOrderByFirstNameAsc(accountId, UserRole.OPERATOR)
                .stream()
                .map(OperatorResponse::from)
                .toList();
    }
}