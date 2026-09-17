package com.net2rent.net2rent_backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.net2rent.net2rent_backend.model.enums.Permission;
import com.net2rent.net2rent_backend.model.enums.UserRole;
import org.junit.jupiter.api.Test;

class RolePermissionsTest {

    @Test
    void adminHasFullPermissionSetAndNeverSelfAssign() {
        var perms = RolePermissions.forRole(UserRole.ADMIN);
        assertThat(perms).containsExactlyInAnyOrder(
                Permission.VIEW_ALL_INCIDENTS,
                Permission.REGISTER_PHONE_INCIDENT,
                Permission.ASSIGN_OPERATOR,
                Permission.TRIAGE_INCIDENT,
                Permission.WORK_INCIDENT,
                Permission.RESOLVE_INCIDENT,
                Permission.MANAGE_CHECKLIST,
                Permission.IMPUTE_TIME,
                Permission.WRITE_COMMENT,
                Permission.CLOSE_INCIDENT,
                Permission.REJECT_INCIDENT,
                Permission.VIEW_LODGINGS,
                Permission.MANAGE_LODGINGS,
                Permission.MANAGE_USERS);
        assertThat(perms).doesNotContain(Permission.SELF_ASSIGN_FROM_POOL);
    }

    @Test
    void coordinatorHasOfficeScopeAndCannotManageResources() {
        var perms = RolePermissions.forRole(UserRole.COORDINATOR);
        assertThat(perms).containsExactlyInAnyOrder(
                Permission.VIEW_ALL_INCIDENTS,
                Permission.REGISTER_PHONE_INCIDENT,
                Permission.ASSIGN_OPERATOR,
                Permission.TRIAGE_INCIDENT,
                Permission.WORK_INCIDENT,
                Permission.RESOLVE_INCIDENT,
                Permission.MANAGE_CHECKLIST,
                Permission.IMPUTE_TIME,
                Permission.WRITE_COMMENT,
                Permission.CLOSE_INCIDENT,
                Permission.REJECT_INCIDENT,
                Permission.VIEW_LODGINGS);
    }

    @Test
    void operatorHasOwnScopeOnly() {
        var perms = RolePermissions.forRole(UserRole.OPERATOR);
        assertThat(perms).containsExactlyInAnyOrder(
                Permission.SELF_ASSIGN_FROM_POOL,
                Permission.WORK_INCIDENT,
                Permission.RESOLVE_INCIDENT,
                Permission.MANAGE_CHECKLIST,
                Permission.IMPUTE_TIME,
                Permission.WRITE_COMMENT,
                Permission.VIEW_LODGINGS);
    }

    @Test
    void nullRoleReturnsEmptySet() {
        assertThat(RolePermissions.forRole(null)).isEmpty();
    }

    @Test
    void returnedSetIsImmutable() {
        var perms = RolePermissions.forRole(UserRole.ADMIN);
        assertThatThrownBy(() -> perms.add(Permission.SELF_ASSIGN_FROM_POOL))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}