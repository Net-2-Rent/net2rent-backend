package com.net2rent.net2rent_backend.controller;

import com.net2rent.net2rent_backend.dto.*;
import com.net2rent.net2rent_backend.dto.request.CreateUserRequest;
import com.net2rent.net2rent_backend.dto.request.ResetPasswordRequest;
import com.net2rent.net2rent_backend.dto.request.UpdateUserRequest;
import com.net2rent.net2rent_backend.dto.response.UserResponse;
import com.net2rent.net2rent_backend.model.enums.UserRole;
import com.net2rent.net2rent_backend.security.AuthUser;
import com.net2rent.net2rent_backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // --- Existente (sin cambios) ---

    @GetMapping("/operators")
    @PreAuthorize("hasAuthority('ASSIGN_OPERATOR')")
    public List<OperatorResponse> operators(@AuthenticationPrincipal AuthUser user) {
        return userService.listAssignableOperators(user.accountId());
    }

    // --- Nuevos ---

    @GetMapping
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    public List<UserResponse> list(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) Boolean active) {
        return userService.list(user.accountId(), role, active);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    public ResponseEntity<UserResponse> create(
            @AuthenticationPrincipal AuthUser user,
            @Valid @RequestBody CreateUserRequest request) {
        UserResponse created = userService.create(user.accountId(), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    public UserResponse update(
            @AuthenticationPrincipal AuthUser user,
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        return userService.update(user.accountId(), id, request);
    }

    @PatchMapping("/{id}/password")
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    public ResponseEntity<Void> resetPassword(
            @AuthenticationPrincipal AuthUser user,
            @PathVariable Long id,
            @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(user.accountId(), id, request);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('MANAGE_USERS')")
    public UserResponse deactivate(
            @AuthenticationPrincipal AuthUser user,
            @PathVariable Long id) {
        return userService.deactivate(user.accountId(), user.userId(), id);
    }
}