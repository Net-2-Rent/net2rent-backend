package com.net2rent.net2rent_backend.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.net2rent.net2rent_backend.exception.ForbiddenException;
import com.net2rent.net2rent_backend.model.AppUser;
import com.net2rent.net2rent_backend.model.Incident;
import org.junit.jupiter.api.Test;

class IncidentAccessPolicyTest {

    private final IncidentAccessPolicy policy = new IncidentAccessPolicy();

    private AuthUser operator(Long userId) {
        return new AuthUser(userId, 1L, "op@x.com", "OPERATOR");
    }

    @Test
    void assignedOperatorCanAct() {
        AppUser me = AppUser.builder().id(7L).build();
        Incident inc = Incident.builder().assignee(me).build();

        assertThatCode(() -> policy.ensureCanActOn(inc, operator(7L)))
                .doesNotThrowAnyException();
    }

    @Test
    void operatorOnAnotherIncidentIsForbidden() {
        AppUser other = AppUser.builder().id(99L).build();
        Incident inc = Incident.builder().assignee(other).build();

        assertThatThrownBy(() -> policy.ensureCanActOn(inc, operator(7L)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void operatorOnPoolIncidentIsForbidden() {
        Incident inc = Incident.builder().assignee(null).build();

        assertThatThrownBy(() -> policy.ensureCanActOn(inc, operator(7L)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void coordinatorCanActOnAny() {
        AppUser other = AppUser.builder().id(99L).build();
        Incident inc = Incident.builder().assignee(other).build();
        AuthUser coord = new AuthUser(3L, 1L, "coord@x.com", "COORDINATOR");

        assertThatCode(() -> policy.ensureCanActOn(inc, coord))
                .doesNotThrowAnyException();
    }
}