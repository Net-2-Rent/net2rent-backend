package com.net2rent.net2rent_backend.dto.response;

import com.net2rent.net2rent_backend.model.Incident;

public record GuestIncidentResponse(String code) {
    public static GuestIncidentResponse from(Incident incident) {
        return new GuestIncidentResponse(incident.getCode());
    }
}
