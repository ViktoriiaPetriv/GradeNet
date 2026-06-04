package org.bachelor.integrationservice.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GradeComparisonCellDTO {
    private int disciplineIndex;
    private Integer fileGrade;
    private Integer existingGrade;
    private Long existingEntryId;
    private boolean hasDiff;
    private boolean isNew;
}
