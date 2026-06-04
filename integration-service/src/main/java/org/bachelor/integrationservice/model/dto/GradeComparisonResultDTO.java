package org.bachelor.integrationservice.model.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GradeComparisonResultDTO {
    private String groupName;
    private String academicYear;
    private List<GradeComparisonDisciplineDTO> disciplines;
    private List<GradeComparisonStudentDTO> students;
    private boolean hasAnyDiff;
}
