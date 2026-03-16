package org.bachelor.userservice.utils;

import org.bachelor.userservice.model.dto.AuthenticatedUser;
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

    public static Long getCurrentUserId() {
        return getCurrentUser().getUserId();
    }

    public static boolean isAdmin() {
        return getCurrentUser().isAdmin();
    }

    public static boolean isCurrentUser(Long id) {
        return getCurrentUserId().equals(id);
    }

    public static void requireAdmin() {
        if (!isAdmin()) {
            throw new AccessDeniedException("Недостатньо прав");
        }
    }

    public static void requireAdminOrSelf(Long id) {
        if (!isAdmin() && !isCurrentUser(id)) {
            throw new AccessDeniedException("Ви можете виконувати цю дію тільки для свого профілю");
        }
    }

    public static void requireAdminOrManagerOfOrg(Long orgId) {
        AuthenticatedUser user = getCurrentUser();
        if (user.isAdmin()) return;
        if (user.isManager() && orgId.equals(user.getOrgId())) return;
        throw new AccessDeniedException("Немає доступу до організації: " + orgId);
    }

    public static void requireAdminOrManager() {
        AuthenticatedUser user = getCurrentUser();
        if (!user.isAdmin() && !user.isManager()) {
            throw new AccessDeniedException("Недостатньо прав");
        }
    }

}