package org.bachelor.gradeservice.repository;

import org.bachelor.gradeservice.model.entity.EntryResult;
import org.bachelor.gradeservice.model.entity.EntryStatus;
import org.bachelor.gradeservice.model.entity.GradeBookEntry;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

// GradeBookEntrySpecification.java
public class GradeBookEntrySpecification {

    public static Specification<GradeBookEntry> byBookNumberId(Long id) {
        return (root, q, cb) -> id == null ? null : cb.equal(root.get("bookNumberId"), id);
    }

    public static Specification<GradeBookEntry> bySpecialtyDisciplineId(Long id) {
        return (root, q, cb) -> id == null ? null : cb.equal(root.get("specialtyDiscipline").get("id"), id);
    }

    public static Specification<GradeBookEntry> byProfessorId(Long id) {
        return (root, q, cb) -> id == null ? null : cb.equal(root.get("professorId"), id);
    }

    public static Specification<GradeBookEntry> byAcademicYear(String year) {
        return (root, q, cb) -> year == null ? null : cb.equal(root.get("academicYear"), year);
    }

    public static Specification<GradeBookEntry> byStatus(EntryStatus status) {
        return (root, q, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<GradeBookEntry> byResult(EntryResult result) {
        return (root, q, cb) -> result == null ? null : cb.equal(root.get("result"), result);
    }

    public static Specification<GradeBookEntry> bySemester(Integer semester) {
        return (root, q, cb) -> semester == null ? null : cb.equal(root.get("semester"), semester);
    }

    public static Specification<GradeBookEntry> byAcademicYears(List<String> academicYears) {
        return (root, query, cb) ->
                academicYears == null || academicYears.isEmpty() ? null :
                        root.get("academicYear").in(academicYears);
    }
}
