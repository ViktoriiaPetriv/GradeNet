package org.bachelor.gradeservice.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.bachelor.gradeservice.model.entity.AssessmentType;

import java.time.LocalDateTime;

@Data
public class GradeUpdateDTO {

    @NotNull
    private LocalDateTime assessmentDate;

    @NotNull
    @Min(0) @Max(100)
    private Integer universityGrade;

    @NotNull
    private AssessmentType assessmentType;
}
