package com.net2rent.net2rent_backend.controller;

import com.net2rent.net2rent_backend.dto.OperatorResponse;
import com.net2rent.net2rent_backend.security.AuthUser;
import com.net2rent.net2rent_backend.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/operators")
    @PreAuthorize("hasAuthority('ASSIGN_OPERATOR')")
    public List<OperatorResponse> operators(@AuthenticationPrincipal AuthUser user) {
        return userService.listAssignableOperators(user.accountId());
    }
}