package com.net2rent.net2rent_backend.controller;

import com.net2rent.net2rent_backend.dto.request.CreateGuestIncidentRequest;
import com.net2rent.net2rent_backend.dto.response.GuestIncidentDetailResponse;
import com.net2rent.net2rent_backend.dto.response.GuestIncidentResponse;
import com.net2rent.net2rent_backend.dto.response.GuestIncidentSummaryResponse;
import com.net2rent.net2rent_backend.security.GuestPrincipal;
import com.net2rent.net2rent_backend.service.IncidentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/guest")
public class GuestIncidentController {

    private final IncidentService incidentService;

    public GuestIncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @PostMapping("/incidents")
    @PreAuthorize("hasRole('GUEST')")
    public ResponseEntity<GuestIncidentResponse> registerGuestIncident(
            @Valid @RequestBody CreateGuestIncidentRequest request,
            @AuthenticationPrincipal GuestPrincipal guest) {

        GuestIncidentResponse created = incidentService.registerGuestIncident(request, guest);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/incidents")
    @PreAuthorize("hasRole('GUEST')")
    public List<GuestIncidentSummaryResponse> list(@AuthenticationPrincipal GuestPrincipal guest) {
        return incidentService.listByLodging(guest.lodgingId());
    }

    @GetMapping("/incidents/{id}")
    @PreAuthorize("hasRole('GUEST')")
    public GuestIncidentDetailResponse detail(@PathVariable Long id,
                                              @AuthenticationPrincipal GuestPrincipal guest) {
        return GuestIncidentDetailResponse.from(
                incidentService.getOwnedByLodgingOr404(id, guest.lodgingId()));
    }
}
