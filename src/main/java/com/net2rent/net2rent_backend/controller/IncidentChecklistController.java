package com.net2rent.net2rent_backend.controller;

import com.net2rent.net2rent_backend.dto.request.CreateChecklistItemRequest;
import com.net2rent.net2rent_backend.dto.request.ReorderChecklistRequest;
import com.net2rent.net2rent_backend.dto.request.UpdateChecklistItemRequest;
import com.net2rent.net2rent_backend.dto.response.ChecklistItemResponse;
import com.net2rent.net2rent_backend.security.AuthUser;
import com.net2rent.net2rent_backend.service.IncidentChecklistService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incidents/{id}/checklist")
public class IncidentChecklistController {

    private final IncidentChecklistService incidentChecklistService;

    public IncidentChecklistController(IncidentChecklistService incidentChecklistService) {
        this.incidentChecklistService = incidentChecklistService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('MANAGE_CHECKLIST')")
    public List<ChecklistItemResponse> listChecklist(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUser user) {
        return incidentChecklistService.list(id, user);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MANAGE_CHECKLIST')")
    public List<ChecklistItemResponse> addChecklistItem(
            @PathVariable Long id,
            @Valid @RequestBody CreateChecklistItemRequest request,
            @AuthenticationPrincipal AuthUser user) {
        return incidentChecklistService.addItem(id, request, user);
    }

    @PatchMapping("/{itemId}")
    @PreAuthorize("hasAuthority('MANAGE_CHECKLIST')")
    public List<ChecklistItemResponse> setChecklistItemDone(
            @PathVariable Long id,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateChecklistItemRequest request,
            @AuthenticationPrincipal AuthUser user) {
        return incidentChecklistService.setDone(id, itemId, request, user);
    }

    @DeleteMapping("/{itemId}")
    @PreAuthorize("hasAuthority('MANAGE_CHECKLIST')")
    public List<ChecklistItemResponse> deleteChecklistItem(
            @PathVariable Long id,
            @PathVariable Long itemId,
            @AuthenticationPrincipal AuthUser user) {
        return incidentChecklistService.deleteItem(id, itemId, user);
    }

    @PatchMapping("/order")
    @PreAuthorize("hasAuthority('MANAGE_CHECKLIST')")
    public List<ChecklistItemResponse> reorderChecklist(
            @PathVariable Long id,
            @Valid @RequestBody ReorderChecklistRequest request,
            @AuthenticationPrincipal AuthUser user) {
        return incidentChecklistService.reorder(id, request, user);
    }
}
