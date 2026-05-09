package org.bachelor.userservice.repository;

import org.bachelor.userservice.model.entity.Role;
import org.bachelor.userservice.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.organizations WHERE u.email = :email")
    Optional<User> findByEmailWithOrgs(@Param("email") String email);

    boolean existsByRole(Role role);

    @Query("SELECT u FROM User u WHERE LOWER(CONCAT(u.firstName, ' ', u.lastName)) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "ORDER BY CONCAT(u.lastName, ' ', u.firstName)")
    java.util.List<User> search(@Param("query") String query);
}
