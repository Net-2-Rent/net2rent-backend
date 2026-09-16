package com.net2rent.net2rent_backend.controller;

import com.net2rent.net2rent_backend.dto.request.*;
import com.net2rent.net2rent_backend.dto.response.*;
import com.net2rent.net2rent_backend.model.IncidentImage;
import com.net2rent.net2rent_backend.model.enums.IncidentCategory;
import com.net2rent.net2rent_backend.model.enums.IncidentPriority;
import com.net2rent.net2rent_backend.model.enums.IncidentStatus;
import com.net2rent.net2rent_backend.model.enums.OperatorScope;
import com.net2rent.net2rent_backend.repository.spec.SortField;
import com.net2rent.net2rent_backend.security.AuthUser;
import com.net2rent.net2rent_backend.service.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentService incidentService;
    private final IncidentExecutionService incidentExecutionService;

    public IncidentController(IncidentService incidentService,
                              IncidentExecutionService incidentExecutionService) {
        this.incidentService = incidentService;
        this.incidentExecutionService = incidentExecutionService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public IncidentListResponse list(
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) IncidentPriority priority,
            @RequestParam(required = false) IncidentCategory category,
            @RequestParam(required = false) Long lodgingId,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) Boolean unassigned,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate openedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate openedTo,
            @RequestParam(defaultValue = "openedAt") String sort,
            @RequestParam(defaultValue = "desc") String dir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) OperatorScope scope,
            @AuthenticationPrincipal AuthUser user) {

        IncidentFilter filter = new IncidentFilter(
                status, priority, category, lodgingId, assigneeId, unassigned, openedFrom, openedTo);

        SortField sortField = "priority".equalsIgnoreCase(sort) ? SortField.PRIORITY : SortField.OPENED_AT;
        Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;

        int safeSize = Math.clamp(size, 1, 100);
        int safePage = Math.max(page, 0);

        Pageable pageable = PageRequest.of(safePage, safeSize);

        return incidentService.list(filter, sortField, direction, pageable, user, scope);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public IncidentResponse getOne(@PathVariable Long id,
                                   @AuthenticationPrincipal AuthUser user) {
        return incidentService.getDetail(id, user);
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

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('REJECT_INCIDENT')")
    public IncidentResponse reject(@PathVariable Long id,
                                   @Valid @RequestBody RejectIncidentRequest request,
                                   @AuthenticationPrincipal AuthUser user) {
        return incidentExecutionService.reject(id, request, user);
    }

    @PatchMapping("/{id}/claim")
    @PreAuthorize("hasAuthority('SELF_ASSIGN_FROM_POOL')")
    public IncidentResponse claim(@PathVariable Long id,
                                  @AuthenticationPrincipal AuthUser user) {
        return incidentExecutionService.claim(id, user);
    }

    @PatchMapping("/{id}/close")
    @PreAuthorize("hasAuthority('CLOSE_INCIDENT')")
    public IncidentResponse close(@PathVariable Long id,
                                  @AuthenticationPrincipal AuthUser user) {
        return incidentExecutionService.close(id, user);
    }

    @PatchMapping("/{id}/start")
    @PreAuthorize("hasAuthority('WORK_INCIDENT')")
    public IncidentResponse start(@PathVariable Long id,
                                  @AuthenticationPrincipal AuthUser user) {
        return incidentExecutionService.start(id, user);
    }

    @PatchMapping("/{id}/pause")
    @PreAuthorize("hasAuthority('WORK_INCIDENT')")
    public IncidentResponse pause(@PathVariable Long id,
                                  @Valid @RequestBody PauseIncidentRequest request,
                                  @AuthenticationPrincipal AuthUser user) {
        return incidentExecutionService.pause(id, request, user);
    }

    @PatchMapping("/{id}/resume")
    @PreAuthorize("hasAuthority('WORK_INCIDENT')")
    public IncidentResponse resume(@PathVariable Long id,
                                   @AuthenticationPrincipal AuthUser user) {
        return incidentExecutionService.resume(id, user);
    }

    @PatchMapping("/{id}/resolve")
    @PreAuthorize("hasAuthority('RESOLVE_INCIDENT')")
    public IncidentResponse resolve(@PathVariable Long id,
                                    @Valid @RequestBody ResolveIncidentRequest request,
                                    @AuthenticationPrincipal AuthUser user) {
        return incidentExecutionService.resolve(id, request, user);
    }

    @PatchMapping("/{id}/assignee")
    @PreAuthorize("hasAuthority('ASSIGN_OPERATOR')")
    public IncidentResponse assignOperator(@PathVariable Long id,
            @Valid @RequestBody AssignOperatorRequest request,
            @AuthenticationPrincipal AuthUser user) {
        return incidentExecutionService.assignOperator(id, request, user);
    }

    @GetMapping("/{incidentId}/images/{imageId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> getImage(@PathVariable Long incidentId,
                                           @PathVariable Long imageId,
                                           @RequestParam(defaultValue = "false") boolean download,
                                           @AuthenticationPrincipal AuthUser user) {
        IncidentImage img = incidentService.getImageForView(incidentId, imageId, user);

        String ext = "image/png".equals(img.getContentType()) ? "png" : "jpg";
        ContentDisposition disposition = ContentDisposition
                .builder(download ? "attachment" : "inline")
                .filename("incidencia-" + incidentId + "-" + imageId + "." + ext)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(img.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(img.getData());
    }
}