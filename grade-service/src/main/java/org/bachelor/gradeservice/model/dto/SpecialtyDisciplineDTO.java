package org.bachelor.gradeservice.model.dto;

import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class SpecialtyDisciplineDTO {
    private Long id;
    private Long specialtyId;
    private DisciplineDTO discipline;
    private Set<HoursDTO> hours;
}
