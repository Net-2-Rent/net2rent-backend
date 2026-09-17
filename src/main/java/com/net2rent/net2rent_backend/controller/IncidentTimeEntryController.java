package com.net2rent.net2rent_backend.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;

import com.net2rent.net2rent_backend.dto.request.CreateTimeEntryRequest;
import com.net2rent.net2rent_backend.dto.response.TimeEntryResponse;
import com.net2rent.net2rent_backend.security.AuthUser;
import com.net2rent.net2rent_backend.service.IncidentTimeEntryService;

import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/incidents/{id}/time-entries")
public class IncidentTimeEntryController {

    private final IncidentTimeEntryService incidentTimeEntryService;

    public IncidentTimeEntryController(IncidentTimeEntryService incidentTimeEntryService) {
        this.incidentTimeEntryService = incidentTimeEntryService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('IMPUTE_TIME')")
    public List<TimeEntryResponse> list(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUser user) {
        return incidentTimeEntryService.list(id, user);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('IMPUTE_TIME')")
    public List<TimeEntryResponse> add(
            @PathVariable Long id,
            @Valid @RequestBody CreateTimeEntryRequest request,
            @AuthenticationPrincipal AuthUser user) {
        return incidentTimeEntryService.add(id, request, user);
    }

    @PatchMapping("/{entryId}")
    @PreAuthorize("hasAuthority('IMPUTE_TIME')")
    public List<TimeEntryResponse> update(
            @PathVariable Long id,
            @PathVariable Long entryId,
            @Valid @RequestBody CreateTimeEntryRequest request,
            @AuthenticationPrincipal AuthUser user) {
        return incidentTimeEntryService.update(id, entryId, request, user);
    }

    @DeleteMapping("/{entryId}")
    @PreAuthorize("hasAuthority('IMPUTE_TIME')")
    public List<TimeEntryResponse> delete(
            @PathVariable Long id,
            @PathVariable Long entryId,
            @AuthenticationPrincipal AuthUser user) {
        return incidentTimeEntryService.delete(id, entryId, user);
    }
}
