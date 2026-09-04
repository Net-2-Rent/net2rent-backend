package com.net2rent.net2rent_backend.controller;

import com.net2rent.net2rent_backend.dto.ClassifyIncidentRequest;
import com.net2rent.net2rent_backend.dto.CorrectIncidentTextRequest;
import com.net2rent.net2rent_backend.dto.IncidentResponse;
import com.net2rent.net2rent_backend.dto.request.CreateCommentRequest;
import com.net2rent.net2rent_backend.dto.request.CreatePhoneIncidentRequest;
import com.net2rent.net2rent_backend.dto.response.TimelineItemResponse;
import com.net2rent.net2rent_backend.security.AuthUser;
import com.net2rent.net2rent_backend.service.IncidentCommentService;
import com.net2rent.net2rent_backend.service.IncidentService;
import com.net2rent.net2rent_backend.service.IncidentTimelineService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
    private final IncidentTimelineService incidentTimelineService;
    private final IncidentCommentService incidentCommentService;

    public IncidentController(IncidentService incidentService, IncidentTimelineService incidentTimelineService, IncidentCommentService incidentCommentService) {
        this.incidentService = incidentService;
        this.incidentTimelineService = incidentTimelineService;
        this.incidentCommentService = incidentCommentService;
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

    @PatchMapping("/{id}/classification")
    @PreAuthorize("hasAuthority('TRIAGE_INCIDENT')")
    public IncidentResponse classify(@PathVariable Long id,
        @Valid @RequestBody ClassifyIncidentRequest request,
        @AuthenticationPrincipal AuthUser user) {
            return incidentService.classify(id, request, user);
    }

    @PatchMapping("/{id}/urgent")
    @PreAuthorize("hasAuthority('TRIAGE_INCIDENT')")
    public IncidentResponse markUrgent(@PathVariable Long id,
        @AuthenticationPrincipal AuthUser user) {
            return incidentService.markUrgent(id, user);
    }

    @PatchMapping("/{id}/text")
    @PreAuthorize("hasAuthority('TRIAGE_INCIDENT')")
    public IncidentResponse correctText(@PathVariable Long id,
        @Valid @RequestBody CorrectIncidentTextRequest request,
        @AuthenticationPrincipal AuthUser user) {
            return incidentService.correctText(id, request, user);
    }

    @GetMapping("/{id}/timeline")
    @PreAuthorize("isAuthenticated()")
    public List<TimelineItemResponse> timeline(@PathVariable Long id,
                                               @AuthenticationPrincipal AuthUser user) {
        return incidentTimelineService.getTimeline(id, user);
    }

    @PostMapping("/{id}/comments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TimelineItemResponse> addComment(
            @PathVariable Long id,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal AuthUser user) {
        TimelineItemResponse created = incidentCommentService.addComment(id, request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}