package org.bachelor.gradeservice.model.dto;

import org.bachelor.gradeservice.model.entity.AssessmentType;
import org.bachelor.gradeservice.model.entity.EctsGrade;
import org.bachelor.gradeservice.model.entity.GradeState;
import org.bachelor.gradeservice.model.entity.NationalGrade;

import java.time.Instant;

public record GradeDTO(
        Long id,
        Long studentId,
        Long specialtyDisciplineId,
        Integer attempt,
        String academicYear,
        Integer semester,
        Instant assessmentDate,
        Integer universityGrade,
        NationalGrade nationalGrade,
        EctsGrade ectsGrade,
        AssessmentType assessment,
        GradeState state
) {}
