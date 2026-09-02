package com.net2rent.net2rent_backend.controller;

import com.net2rent.net2rent_backend.dto.IncidentResponse;
import com.net2rent.net2rent_backend.dto.request.CreatePhoneIncidentRequest;
import com.net2rent.net2rent_backend.security.AuthUser;
import com.net2rent.net2rent_backend.service.IncidentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
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

    @PostMapping
    @PreAuthorize("hasAuthority('REGISTER_PHONE_INCIDENT')")
    public ResponseEntity<IncidentResponse> registerPhoneIncident(
            @Valid @RequestBody CreatePhoneIncidentRequest request,
            @AuthenticationPrincipal AuthUser user) {

        IncidentResponse created = incidentService.registerPhoneIncident(request, user);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }
}