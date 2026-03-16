package org.bachelor.userservice.repository;

import org.bachelor.userservice.model.entity.User;
import org.bachelor.userservice.model.entity.UserOrganization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserOrganizationRepository extends JpaRepository<UserOrganization, Long> {

    Optional<UserOrganization> findByUser(User user);

    @Query("SELECT uo.user.id FROM UserOrganization uo WHERE uo.orgId IN :orgIds")
    List<Long> findUserIdsByOrgIdIn(@Param("orgIds") List<Long> orgIds);
}
