package com.net2rent.net2rent_backend.controller;

import com.net2rent.net2rent_backend.dto.request.CreateCommentRequest;
import com.net2rent.net2rent_backend.dto.response.TimelineItemResponse;
import com.net2rent.net2rent_backend.security.AuthUser;
import com.net2rent.net2rent_backend.service.IncidentCommentService;
import com.net2rent.net2rent_backend.service.IncidentTimelineService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incidents/{id}")
public class IncidentTimelineController {

    private final IncidentTimelineService incidentTimelineService;
    private final IncidentCommentService incidentCommentService;

    public IncidentTimelineController(IncidentTimelineService incidentTimelineService, IncidentCommentService incidentCommentService) {
        this.incidentTimelineService = incidentTimelineService;
        this.incidentCommentService = incidentCommentService;
    }

    @GetMapping("/timeline")
    @PreAuthorize("isAuthenticated()")
    public List<TimelineItemResponse> timeline(@PathVariable Long id,
                                               @AuthenticationPrincipal AuthUser user) {
        return incidentTimelineService.getTimeline(id, user);
    }

    @PostMapping("/comments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TimelineItemResponse> addComment(
            @PathVariable Long id,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal AuthUser user) {
        TimelineItemResponse created = incidentCommentService.addComment(id, request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
