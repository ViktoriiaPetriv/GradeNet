package org.bachelor.userservice.repository;

import org.bachelor.userservice.model.entity.StudentGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentGroupRepository extends JpaRepository<StudentGroup, Long> {
    Optional<StudentGroup> findByName(String name);
    boolean existsByName(String name);
    Page<StudentGroup> findByNameContainingIgnoreCase(String name, Pageable pageable);

    @Query(value = "SELECT DISTINCT m.studentGroup FROM StudentGroupMember m WHERE m.bookNumber.specialtyId = :specialtyId ORDER BY m.studentGroup.name ASC",
           countQuery = "SELECT COUNT(DISTINCT m.studentGroup) FROM StudentGroupMember m WHERE m.bookNumber.specialtyId = :specialtyId")
    Page<StudentGroup> findBySpecialtyId(@Param("specialtyId") Long specialtyId, Pageable pageable);

    @Query(value = "SELECT DISTINCT m.studentGroup FROM StudentGroupMember m WHERE m.bookNumber.specialtyId = :specialtyId AND LOWER(m.studentGroup.name) LIKE LOWER(CONCAT('%', :name, '%')) ORDER BY m.studentGroup.name ASC",
           countQuery = "SELECT COUNT(DISTINCT m.studentGroup) FROM StudentGroupMember m WHERE m.bookNumber.specialtyId = :specialtyId AND LOWER(m.studentGroup.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<StudentGroup> findBySpecialtyIdAndNameContaining(@Param("specialtyId") Long specialtyId, @Param("name") String name, Pageable pageable);
}
