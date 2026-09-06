package com.net2rent.net2rent_backend.controller;

import com.net2rent.net2rent_backend.dto.LodgingResponse;
import com.net2rent.net2rent_backend.dto.request.LodgingRequest;
import com.net2rent.net2rent_backend.security.AuthUser;
import com.net2rent.net2rent_backend.service.LodgingService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/lodgings")
public class LodgingController {

    private final LodgingService lodgingService;

    public LodgingController(LodgingService lodgingService) {
        this.lodgingService = lodgingService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_LODGINGS')")
    public List<LodgingResponse> list(@AuthenticationPrincipal AuthUser user) {
        return lodgingService.listForAccount(user.accountId());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VIEW_LODGINGS')")
    public LodgingResponse getOne(@PathVariable Long id,
                                  @AuthenticationPrincipal AuthUser user) {
        return lodgingService.getForAccount(id, user.accountId());
    }

     @PostMapping
    @PreAuthorize("hasAuthority('MANAGE_LODGINGS')")
    public ResponseEntity<LodgingResponse> create(@Valid @RequestBody LodgingRequest request,
                                                   @AuthenticationPrincipal AuthUser user) {
        LodgingResponse response = lodgingService.create(user.accountId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_LODGINGS')")
    public LodgingResponse update(@PathVariable Long id,
                                  @Valid @RequestBody LodgingRequest request,
                                  @AuthenticationPrincipal AuthUser user) {
        return lodgingService.update(user.accountId(), id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_LODGINGS')")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
        lodgingService.deactivate(user.accountId(), id);
        return ResponseEntity.noContent().build();
    }
}