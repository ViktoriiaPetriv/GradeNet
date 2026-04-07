package org.bachelor.gradeservice.repository;

import org.bachelor.gradeservice.model.entity.Hours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HoursRepository extends JpaRepository<Hours, Long> {
    Optional<Hours> findBySpecialtyDisciplineId(Long specialtyDisciplineId);
    boolean existsBySpecialtyDisciplineId(Long specialtyDisciplineId);
}
