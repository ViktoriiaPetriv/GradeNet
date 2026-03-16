package org.bachelor.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bachelor.userservice.config.OrgServiceClient;
import org.bachelor.userservice.model.dto.AuthenticatedUser;
import org.bachelor.userservice.repository.BookNumberRepository;
import org.bachelor.userservice.repository.UserOrganizationRepository;
import org.bachelor.userservice.utils.SecurityUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccessControlService {

    private final OrgServiceClient orgServiceClient;
    private final BookNumberRepository bookNumberRepository;
    private final UserOrganizationRepository userOrganizationRepository;

    public void requireAdminOrManagerOfSpecialty(Long specialtyId) {
        AuthenticatedUser user = SecurityUtils.getCurrentUser();
        if (user.isAdmin()) return;
        if (user.isManager()) {
            List<Long> specialtyIds = orgServiceClient
                    .getSpecialtyIdsByOrgIds(List.of(user.getOrgId()));
            if (specialtyIds.contains(specialtyId)) return;
            throw new AccessDeniedException("Немає доступу до цієї залікової книжки");
        }
        throw new AccessDeniedException("Недостатньо прав");
    }

    public List<Long> getManagerSpecialtyIds() {
        AuthenticatedUser user = SecurityUtils.getCurrentUser();
        if (!user.isManager()) throw new AccessDeniedException("Недостатньо прав");
        return orgServiceClient.getSpecialtyIdsByOrgIds(List.of(user.getOrgId()));
    }

    public Set<Long> getAllowedUserIdsForManager() {
        AuthenticatedUser user = SecurityUtils.getCurrentUser();
        if (!user.isManager()) throw new AccessDeniedException("Недостатньо прав");

        Long orgId = user.getOrgId();

        List<Long> specialtyIds = orgServiceClient.getSpecialtyIdsByOrgIds(List.of(orgId));
        List<Long> studentIds = bookNumberRepository.findDistinctStudentIdsBySpecialtyIdIn(specialtyIds);
        List<Long> professorIds = userOrganizationRepository.findUserIdsByOrgIdIn(List.of(orgId));

        Set<Long> allIds = new HashSet<>(studentIds);
        allIds.addAll(professorIds);
        return allIds;
    }

    public void requireAdminOrManagerOfUser(Long userId) {
        AuthenticatedUser user = SecurityUtils.getCurrentUser();

        if (user.isAdmin()) return;

        if (user.getUserId().equals(userId)) return;

        if (user.isManager()) {
            if (getAllowedUserIdsForManager().contains(userId)) return;
            throw new AccessDeniedException("Немає доступу до цього користувача");
        }

        throw new AccessDeniedException("Недостатньо прав");
    }
}