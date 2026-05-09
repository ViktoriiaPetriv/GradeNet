package org.bachelor.gradeservice.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.bachelor.gradeservice.model.entity.AssessmentType;

import java.time.LocalDateTime;

// model/dto/GradeCreateDTO.java
@Data
public class GradeCreateDTO {

    @NotNull
    private Long entryId;

    @NotNull
    private LocalDateTime assessmentDate;

    @Min(0) @Max(100)
    private Integer universityGrade;

    private AssessmentType assessmentType;
}