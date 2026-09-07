package com.net2rent.net2rent_backend.service;

import com.net2rent.net2rent_backend.dto.*;
import com.net2rent.net2rent_backend.dto.request.CreateUserRequest;
import com.net2rent.net2rent_backend.dto.request.ResetPasswordRequest;
import com.net2rent.net2rent_backend.dto.request.UpdateUserRequest;
import com.net2rent.net2rent_backend.dto.response.UserResponse;
import com.net2rent.net2rent_backend.exception.ConflictException;
import com.net2rent.net2rent_backend.exception.NotFoundException;
import com.net2rent.net2rent_backend.model.Account;
import com.net2rent.net2rent_backend.model.AppUser;
import com.net2rent.net2rent_backend.model.enums.IncidentStatus;
import com.net2rent.net2rent_backend.model.enums.UserRole;
import com.net2rent.net2rent_backend.repository.AccountRepository;
import com.net2rent.net2rent_backend.repository.IncidentRepository;
import com.net2rent.net2rent_backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final IncidentRepository incidentRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       AccountRepository accountRepository,
                       IncidentRepository incidentRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.incidentRepository = incidentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // --- Existente (sin cambios) ---

    @Transactional(readOnly = true)
    public List<OperatorResponse> listAssignableOperators(Long accountId) {
        return userRepository
                .findByAccount_IdAndRoleAndActiveTrueOrderByFirstNameAsc(accountId, UserRole.OPERATOR)
                .stream()
                .map(OperatorResponse::from)
                .toList();
    }

    // --- Nuevos ---

    @Transactional(readOnly = true)
    public List<UserResponse> list(Long accountId, UserRole role, Boolean active) {
        List<AppUser> users = userRepository.findByAccount_IdOrderByFirstNameAsc(accountId);
        return users.stream()
                .filter(u -> role == null || u.getRole() == role)
                .filter(u -> active == null || u.isActive() == active)
                .map(UserResponse::from)
                .toList();
    }

    @Transactional
    public UserResponse create(Long accountId, CreateUserRequest r) {
        if (userRepository.findByEmail(r.email()).isPresent()) {
            throw new ConflictException("Ese email ya está registrado");
        }
        Account account = accountRepository.getReferenceById(accountId);
        AppUser user = AppUser.builder()
                .account(account)
                .firstName(r.firstName())
                .lastName(r.lastName())
                .email(r.email())
                .role(r.role())
                .passwordHash(passwordEncoder.encode(r.password()))
                .active(true)
                .build();
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(Long accountId, Long id, UpdateUserRequest r) {
        AppUser user = userRepository.findByIdAndAccount_Id(id, accountId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        if (r.email() != null && !r.email().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.findByEmail(r.email()).isPresent()) {
                throw new ConflictException("Ese email ya está registrado");
            }
            user.setEmail(r.email());
        }
        user.setFirstName(r.firstName());
        user.setLastName(r.lastName());
        user.setRole(r.role());
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public void resetPassword(Long accountId, Long id, ResetPasswordRequest r) {
        AppUser user = userRepository.findByIdAndAccount_Id(id, accountId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        user.setPasswordHash(passwordEncoder.encode(r.password()));
        userRepository.save(user);
    }

    @Transactional
    public UserResponse deactivate(Long accountId, Long adminUserId, Long id) {
        AppUser target = userRepository.findByIdAndAccount_Id(id, accountId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        // No desactivarse a sí mismo
        if (target.getId().equals(adminUserId)) {
            throw new ConflictException("La cuenta debe tener al menos un administrador activo");
        }

        // Si es ADMIN y es el último, no permitir
        if (target.getRole() == UserRole.ADMIN
                && userRepository.countByAccount_IdAndRoleAndActiveTrue(accountId, UserRole.ADMIN) <= 1) {
            throw new ConflictException("La cuenta debe tener al menos un administrador activo");
        }

        // Si es OPERATOR con incidencias activas, no permitir
        if (target.getRole() == UserRole.OPERATOR) {
            long active = incidentRepository.countByAssignee_IdAndStatusIn(
                    target.getId(),
                    List.of(IncidentStatus.NEW, IncidentStatus.ASSIGNED,
                            IncidentStatus.IN_PROGRESS, IncidentStatus.PAUSED));
            if (active > 0) {
                throw new ConflictException("El operario tiene " + active + " incidencia(s) activa(s) asignada(s) y no puede desactivarse");
            }
        }

        if (target.isActive()) {
            target.setActive(false);
            userRepository.save(target);
        }
        return UserResponse.from(target);
    }
}