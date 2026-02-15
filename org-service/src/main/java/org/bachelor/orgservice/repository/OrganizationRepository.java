package org.bachelor.orgservice.repository;

import org.bachelor.orgservice.model.entity.OrgType;
import org.bachelor.orgservice.model.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    Optional<Organization> findByName(String name);
    boolean existsByParentId(Long id);
    List<Organization> findAllByParentIdAndOrgType(Long parentId, OrgType orgType);
}
