package org.bachelor.gradeservice.model.dto;

import lombok.Data;

@Data
public class DisciplineCreateResponseDTO {
    private Long disciplineId;
    private String name;
    private Long specialtyDisciplineId;
    private Long specialtyId;
    private HoursDTO hours;
}
