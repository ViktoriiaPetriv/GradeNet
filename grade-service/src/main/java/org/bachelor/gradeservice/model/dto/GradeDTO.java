package org.bachelor.gradeservice.model.dto;

import lombok.Data;
import org.bachelor.gradeservice.model.entity.AssessmentType;
import org.bachelor.gradeservice.model.entity.EctsGrade;

import java.time.LocalDateTime;

@Data
public class GradeDTO {
    private Long id;
    private Long entryId;
    private LocalDateTime assessmentDate;
    private Integer universityGrade;
    private String nationalGrade;
    private EctsGrade ectsGrade;
    private AssessmentType assessmentType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
