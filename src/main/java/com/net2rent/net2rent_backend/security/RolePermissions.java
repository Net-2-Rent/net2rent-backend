package com.net2rent.net2rent_backend.security;

import com.net2rent.net2rent_backend.model.enums.Permission;
import com.net2rent.net2rent_backend.model.enums.UserRole;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.net2rent.net2rent_backend.model.enums.Permission.*;

public class RolePermissions {

    private static final Map<UserRole, Set<Permission>> MATRIX = new EnumMap<>(UserRole.class);

    static {
        Set<Permission> operator = EnumSet.of(
                SELF_ASSIGN_FROM_POOL,
                WORK_INCIDENT,
                RESOLVE_INCIDENT,
                MANAGE_CHECKLIST,
                WRITE_COMMENT,
                VIEW_LODGINGS
        );

        Set<Permission> coordinator = EnumSet.of(
                VIEW_ALL_INCIDENTS,
                REGISTER_PHONE_INCIDENT,
                ASSIGN_OPERATOR,
                TRIAGE_INCIDENT,
                WORK_INCIDENT,
                RESOLVE_INCIDENT,
                MANAGE_CHECKLIST,
                WRITE_COMMENT,
                CLOSE_INCIDENT,
                REJECT_INCIDENT,
                VIEW_LODGINGS
        );

        Set<Permission> admin = EnumSet.copyOf(coordinator);
        admin.addAll(EnumSet.of(MANAGE_LODGINGS, MANAGE_USERS));

        MATRIX.put(UserRole.OPERATOR, Collections.unmodifiableSet(operator));
        MATRIX.put(UserRole.COORDINATOR, Collections.unmodifiableSet(coordinator));
        MATRIX.put(UserRole.ADMIN, Collections.unmodifiableSet(admin));
    }

    private RolePermissions() {
    }

    public static Set<Permission> forRole(UserRole role) {
        return MATRIX.getOrDefault(role, Set.of());
    }
}