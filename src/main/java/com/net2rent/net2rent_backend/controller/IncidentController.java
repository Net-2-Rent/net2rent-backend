package com.net2rent.net2rent_backend.controller;

import com.net2rent.net2rent_backend.dto.IncidentResponse;
import com.net2rent.net2rent_backend.security.AuthUser;
import com.net2rent.net2rent_backend.service.IncidentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<IncidentResponse> list(@AuthenticationPrincipal AuthUser user) {
        return incidentService.list(user);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public IncidentResponse getOne(@PathVariable Long id,
                                   @AuthenticationPrincipal AuthUser user) {
        return IncidentResponse.from(
                incidentService.getOwnedByAccountOr404(id, user));
    }
}