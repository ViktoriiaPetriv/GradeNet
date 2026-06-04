package org.bachelor.integrationservice.model.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GradeComparisonStudentDTO {
    private String fullName;
    private Long bookNumberId;
    private List<GradeComparisonCellDTO> cells;
}
