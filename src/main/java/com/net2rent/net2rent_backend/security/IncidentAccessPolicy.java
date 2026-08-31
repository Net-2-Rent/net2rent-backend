package com.net2rent.net2rent_backend.security;

import com.net2rent.net2rent_backend.exception.ForbiddenException;
import com.net2rent.net2rent_backend.model.Incident;
import com.net2rent.net2rent_backend.model.enums.UserRole;
import org.springframework.stereotype.Component;

@Component
public class IncidentAccessPolicy {

    public void ensureCanActOn(Incident incident, AuthUser user) {
        if (!UserRole.OPERATOR.name().equals(user.role())) {
            return;
        }

        boolean isAssignee = incident.getAssignee() != null
                && incident.getAssignee().getId().equals(user.userId());

        if (!isAssignee) {
            throw new ForbiddenException(
                    "No puedes editar una incidencia que no tienes asignada");
        }
    }
}