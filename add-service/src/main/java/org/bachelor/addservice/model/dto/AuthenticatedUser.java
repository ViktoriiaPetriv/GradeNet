package org.bachelor.addservice.model.dto;

import java.util.Objects;

public record AuthenticatedUser(Long userId, String role, Long orgId) {
    public boolean isAdmin() {
        return Objects.equals(role, "ADMIN");
    }

    public boolean isManager() {
        return Objects.equals(role, "MANAGER");
    }

    public boolean isProfessor() {
        return Objects.equals(role, "PROFESSOR");
    }
}
