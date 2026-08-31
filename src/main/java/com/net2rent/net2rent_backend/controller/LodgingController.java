package com.net2rent.net2rent_backend.controller;

import com.net2rent.net2rent_backend.dto.LodgingResponse;
import com.net2rent.net2rent_backend.security.AuthUser;
import com.net2rent.net2rent_backend.service.LodgingService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
}