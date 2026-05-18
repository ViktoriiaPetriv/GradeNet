package org.bachelor.gradeservice.repository;

import org.bachelor.gradeservice.model.entity.EntryResult;
import org.bachelor.gradeservice.model.entity.EntryStatus;
import org.bachelor.gradeservice.model.entity.GradeBookEntry;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GradeBookEntryRepository extends JpaRepository<GradeBookEntry, Long>,
        JpaSpecificationExecutor<GradeBookEntry> {

    boolean existsByBookNumberIdAndSpecialtyDisciplineIdAndAttempt(
            Long bookNumberId, Long specialtyDisciplineId, Integer attempt);

    Optional<GradeBookEntry> findTopByBookNumberIdAndSpecialtyDisciplineIdOrderByAttemptDesc(
            Long bookNumberId, Long specialtyDisciplineId);

    @EntityGraph(attributePaths = {
            "specialtyDiscipline",
            "specialtyDiscipline.discipline",
            "specialtyDiscipline.hours",
            "specialtyDiscipline.hours.template",
            "grades"
    })
    List<GradeBookEntry> findAll(Specification<GradeBookEntry> spec);

    boolean existsByBookNumberIdAndSpecialtyDiscipline_Discipline_IdAndStatus(Long bookNumberId, Long disciplineId, EntryStatus status);

    boolean existsByBookNumberIdAndSpecialtyDiscipline_Discipline_IdAndResult(Long bookNumberId, Long disciplineId, EntryResult result);

    boolean existsByBookNumberIdAndSpecialtyDiscipline_Discipline_IdAndStatusAndSemester(
            Long bookNumberId, Long disciplineId, EntryStatus status, Integer semester);

    boolean existsByBookNumberIdAndSpecialtyDiscipline_Discipline_IdAndResultAndSemester(
            Long bookNumberId, Long disciplineId, EntryResult result, Integer semester);

    Optional<GradeBookEntry> findTopByBookNumberIdAndSpecialtyDisciplineIdAndSemesterOrderByAttemptDesc(
            Long bookNumberId, Long specialtyDisciplineId, Integer semester);
}