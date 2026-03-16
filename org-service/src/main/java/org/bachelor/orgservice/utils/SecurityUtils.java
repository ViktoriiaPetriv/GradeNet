package org.bachelor.orgservice.utils;

import lombok.experimental.UtilityClass;
import org.bachelor.orgservice.model.dto.AuthenticatedUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@UtilityClass
public class SecurityUtils {

    public static AuthenticatedUser getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new AccessDeniedException("Не авторизовано");
        }
        return user;
    }

    public static void requireAdmin() {
        if (!getCurrentUser().isAdmin()) {
            throw new AccessDeniedException("Недостатньо прав");
        }
    }

    public static void requireAdminOrManager() {
        AuthenticatedUser user = getCurrentUser();
        if (!user.isAdmin() && !user.isManager()) {
            throw new AccessDeniedException("Недостатньо прав");
        }
    }
}
