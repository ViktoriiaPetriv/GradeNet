package org.bachelor.integrationservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisciplineCheckItemDTO {
    private int index;
    private String name;
    private int totalHours;
    private int ectsCredits;
    private boolean existsInSystem;
    private Long disciplineId;
    private Long specialtyDisciplineId;
    private Integer semester;
}
