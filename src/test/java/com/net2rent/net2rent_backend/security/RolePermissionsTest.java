package com.net2rent.net2rent_backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.net2rent.net2rent_backend.model.enums.Permission;
import com.net2rent.net2rent_backend.model.enums.UserRole;
import org.junit.jupiter.api.Test;

class RolePermissionsTest {

    @Test
    void adminHas13PermissionsAndNeverSelfAssign() {
        var perms = RolePermissions.forRole(UserRole.ADMIN);
        assertThat(perms).hasSize(13);
        assertThat(perms).doesNotContain(Permission.SELF_ASSIGN_FROM_POOL);
        assertThat(perms).contains(Permission.MANAGE_USERS, Permission.MANAGE_LODGINGS);
    }

    @Test
    void coordinatorHas11AndCannotManageResources() {
        var perms = RolePermissions.forRole(UserRole.COORDINATOR);
        assertThat(perms).hasSize(11);
        assertThat(perms).contains(Permission.CLOSE_INCIDENT, Permission.ASSIGN_OPERATOR);
        assertThat(perms).doesNotContain(
                Permission.MANAGE_USERS,
                Permission.MANAGE_LODGINGS,
                Permission.SELF_ASSIGN_FROM_POOL);
    }

    @Test
    void operatorHas6AndOnlyOwnScope() {
        var perms = RolePermissions.forRole(UserRole.OPERATOR);
        assertThat(perms).hasSize(6);
        assertThat(perms).contains(Permission.SELF_ASSIGN_FROM_POOL, Permission.RESOLVE_INCIDENT);
        assertThat(perms).doesNotContain(Permission.CLOSE_INCIDENT, Permission.MANAGE_USERS);
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