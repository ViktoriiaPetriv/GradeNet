package org.bachelor.integrationservice.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GradeComparisonDisciplineDTO {
    private int index;
    private String name;
}
