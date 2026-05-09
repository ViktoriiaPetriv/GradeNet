package org.bachelor.gradeservice.repository;

import org.bachelor.gradeservice.model.entity.Hours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HoursRepository extends JpaRepository<Hours, Long> {

    List<Hours> findAllBySpecialtyDisciplineId(Long specialtyDisciplineId);

    Optional<Hours> findBySpecialtyDisciplineIdAndAcademicYear(Long specialtyDisciplineId, String academicYear);

    List<Hours> findAllByAcademicYear(String academicYear);

    boolean existsBySpecialtyDisciplineIdAndAcademicYear(Long specialtyDisciplineId, String academicYear);
}
