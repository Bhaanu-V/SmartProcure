package com.smartprocure.entity;

/**
 * User roles for access control and multi-tier approval routing.
 */
public enum Role {
    EMPLOYEE,
    REQUESTER,
    MANAGER,
    FINANCE,
    PROCUREMENT,
    ADMIN;

    public String getAuthority() {
        return "ROLE_" + this.name();
    }
}
