package com.net2rent.net2rent_backend.dto.response;

import com.net2rent.net2rent_backend.model.enums.IncidentStatus;

import java.util.Map;

public record IncidentListResponse(
        PagedResponse<IncidentSummaryResponse> page,
        Map<IncidentStatus, Long> counters
) {
}