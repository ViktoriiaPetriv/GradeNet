package org.bachelor.gradeservice.repository;

import org.bachelor.gradeservice.model.entity.Grade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GradeRepository extends JpaRepository<Grade, Long> {
    List<Grade> findAllBySpecialtyDisciplineId(Long specialtyDisciplineId);
    List<Grade> findAllByStudentId(Long studentId);
    Optional<Grade> findBySpecialtyDisciplineIdAndStudentId(Long specialtyDisciplineId, Long studentId);
    boolean existsBySpecialtyDisciplineIdAndStudentId(Long specialtyDisciplineId, Long studentId);

    @Query("SELECT MAX(g.attempt) FROM Grade g " +
            "WHERE g.specialtyDiscipline.id = :sdId " +
            "AND g.studentId = :studentId")
    Optional<Integer> findMaxAttemptBySpecialtyDisciplineIdAndStudentId(
            @Param("sdId") Long sdId,
            @Param("studentId") Long studentId);

    List<Grade> findAllByStudentIdAndAcademicYearAndSemester(
            Long studentId, String academicYear, Integer semester);
}
