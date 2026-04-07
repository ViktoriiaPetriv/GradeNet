package org.bachelor.gradeservice.utils;

import org.bachelor.gradeservice.model.dto.AuthenticatedUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    public static AuthenticatedUser getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new AccessDeniedException("Користувач не авторизований");
        }
        return user;
    }

    public static void requireAdminOrManager() {
        AuthenticatedUser user = getCurrentUser();
        if (!user.isAdmin() && !user.isManager()) {
            throw new AccessDeniedException("Недостатньо прав");
        }
    }
}
