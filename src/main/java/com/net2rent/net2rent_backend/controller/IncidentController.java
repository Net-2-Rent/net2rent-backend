package com.net2rent.net2rent_backend.controller;

import com.net2rent.net2rent_backend.dto.*;
import com.net2rent.net2rent_backend.dto.request.CreateChecklistItemRequest;
import com.net2rent.net2rent_backend.dto.request.CreateCommentRequest;
import com.net2rent.net2rent_backend.dto.request.IncidentFilter;
import com.net2rent.net2rent_backend.dto.request.ReorderChecklistRequest;
import com.net2rent.net2rent_backend.dto.request.UpdateChecklistItemRequest;
import com.net2rent.net2rent_backend.dto.response.ChecklistItemResponse;
import com.net2rent.net2rent_backend.dto.response.GuestIncidentDetailResponse;
import com.net2rent.net2rent_backend.dto.response.GuestIncidentSummaryResponse;
import com.net2rent.net2rent_backend.dto.request.CreatePhoneIncidentRequest;
import com.net2rent.net2rent_backend.dto.response.IncidentListResponse;
import com.net2rent.net2rent_backend.dto.response.TimelineItemResponse;
import com.net2rent.net2rent_backend.model.enums.IncidentCategory;
import com.net2rent.net2rent_backend.model.enums.IncidentPriority;
import com.net2rent.net2rent_backend.model.enums.IncidentStatus;
import com.net2rent.net2rent_backend.model.enums.OperatorScope;
import com.net2rent.net2rent_backend.repository.spec.SortField;
import com.net2rent.net2rent_backend.security.AuthUser;
import com.net2rent.net2rent_backend.service.*;
import com.net2rent.net2rent_backend.security.GuestPrincipal;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
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
    private final IncidentTimelineService incidentTimelineService;
    private final IncidentCommentService incidentCommentService;
    private final IncidentChecklistService incidentChecklistService;
    private final IncidentExecutionService incidentExecutionService;

    public IncidentController(IncidentService incidentService,
                              IncidentTimelineService incidentTimelineService,
                              IncidentCommentService incidentCommentService,
                              IncidentChecklistService incidentChecklistService,
                              IncidentExecutionService incidentExecutionService) {
        this.incidentService = incidentService;
        this.incidentTimelineService = incidentTimelineService;
        this.incidentCommentService = incidentCommentService;
        this.incidentChecklistService = incidentChecklistService;
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

    @GetMapping("/guest")
    @PreAuthorize("isAuthenticated()")
    public List<GuestIncidentSummaryResponse> guestList(
            @AuthenticationPrincipal GuestPrincipal guest) {
        return incidentService.listByLodging(guest.lodgingId());
    }

    @GetMapping("/guest/{id}")
    @PreAuthorize("isAuthenticated()")
    public GuestIncidentDetailResponse guestDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal GuestPrincipal guest) {
        return GuestIncidentDetailResponse.from(
                incidentService.getOwnedByLodgingOr404(id, guest.lodgingId()));
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
        return incidentService.reject(id, request, user);
    }

    @PatchMapping("/{id}/claim")
    @PreAuthorize("hasAuthority('SELF_ASSIGN_FROM_POOL')")
    public IncidentResponse claim(@PathVariable Long id,
                                  @AuthenticationPrincipal AuthUser user) {
        return incidentService.claim(id, user);
    }

    @PatchMapping("/{id}/close")
    @PreAuthorize("hasAuthority('CLOSE_INCIDENT')")
    public IncidentResponse close(@PathVariable Long id,
                                  @AuthenticationPrincipal AuthUser user) {
        return incidentService.close(id, user);
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

    @GetMapping("/{id}/checklist")
    @PreAuthorize("hasAuthority('MANAGE_CHECKLIST')")
    public List<ChecklistItemResponse> listChecklist(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUser user) {
        return incidentChecklistService.list(id, user);
    }

    @PostMapping("/{id}/checklist")
    @PreAuthorize("hasAuthority('MANAGE_CHECKLIST')")
    public List<ChecklistItemResponse> addChecklistItem(
            @PathVariable Long id,
            @Valid @RequestBody CreateChecklistItemRequest request,
            @AuthenticationPrincipal AuthUser user) {
        return incidentChecklistService.addItem(id, request, user);
    }

    @PatchMapping("/{id}/checklist/{itemId}")
    @PreAuthorize("hasAuthority('MANAGE_CHECKLIST')")
    public List<ChecklistItemResponse> setChecklistItemDone(
            @PathVariable Long id,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateChecklistItemRequest request,
            @AuthenticationPrincipal AuthUser user) {
        return incidentChecklistService.setDone(id, itemId, request, user);
    }

    @DeleteMapping("/{id}/checklist/{itemId}")
    @PreAuthorize("hasAuthority('MANAGE_CHECKLIST')")
    public List<ChecklistItemResponse> deleteChecklistItem(
            @PathVariable Long id,
            @PathVariable Long itemId,
            @AuthenticationPrincipal AuthUser user) {
        return incidentChecklistService.deleteItem(id, itemId, user);
    }

    @PatchMapping("/{id}/checklist/order")
    @PreAuthorize("hasAuthority('MANAGE_CHECKLIST')")
    public List<ChecklistItemResponse> reorderChecklist(
            @PathVariable Long id,
            @Valid @RequestBody ReorderChecklistRequest request,
            @AuthenticationPrincipal AuthUser user) {
        return incidentChecklistService.reorder(id, request, user);
    }

    @PatchMapping("/{id}/assignee")
    @PreAuthorize("hasAuthority('ASSIGN_OPERATOR')")
    public IncidentResponse assignOperator(@PathVariable Long id,
            @Valid @RequestBody AssignOperatorRequest request,
            @AuthenticationPrincipal AuthUser user) {
        return incidentService.assignOperator(id, request, user);
    }
}