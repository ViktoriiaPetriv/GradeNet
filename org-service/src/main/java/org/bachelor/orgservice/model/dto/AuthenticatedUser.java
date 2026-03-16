package org.bachelor.orgservice.model.dto;


import java.util.Objects;

public record AuthenticatedUser(Long userId, String role, Long orgId) {
    public boolean isAdmin() {
        return Objects.equals(role, "ADMIN");
    }

    public boolean isManager() {
        return Objects.equals(role, "MANAGER");
    }

    public boolean isStudent() {
        return Objects.equals(role, "STUDENT");
    }

    public Long getOrgId() { return orgId; }

    public Long getUserId() {
        return userId;
    }
}