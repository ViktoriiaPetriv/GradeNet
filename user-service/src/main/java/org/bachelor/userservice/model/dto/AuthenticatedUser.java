package org.bachelor.userservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bachelor.userservice.model.entity.Role;

@Getter
@AllArgsConstructor
public class AuthenticatedUser {
    private final Long userId;
    private final Role role;
    private final Long orgId;

    public boolean isAdmin() { return role == Role.ADMIN; }
    public boolean isManager() { return role == Role.MANAGER; }
    public boolean isStudent() { return role == Role.STUDENT; }
}