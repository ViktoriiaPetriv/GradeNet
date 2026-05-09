package org.bachelor.gradeservice.repository;

import org.bachelor.gradeservice.model.entity.AssessmentType;
import org.bachelor.gradeservice.model.entity.Grade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GradeRepository extends JpaRepository<Grade, Long> {

    List<Grade> findAllByEntryId(Long entryId);
}
