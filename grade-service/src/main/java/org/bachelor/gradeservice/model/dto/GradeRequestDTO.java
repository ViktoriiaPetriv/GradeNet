package org.bachelor.gradeservice.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.bachelor.gradeservice.model.entity.AssessmentType;
import org.bachelor.gradeservice.model.entity.GradeState;

import java.time.Instant;

public record GradeRequestDTO(
        @NotNull Long studentId,
        @NotNull Long specialtyDisciplineId,
        String academicYear,
        Integer semester,
        Instant assessmentDate,
        @Min(1) @Max(100) Integer universityGrade,
        AssessmentType assessment,
        GradeState state
) {}
