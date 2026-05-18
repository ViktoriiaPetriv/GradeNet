package org.bachelor.integrationservice.model.client;

import lombok.Data;

@Data
public class SpecialtyDisciplineDTO {
    private Long id;
    private Long specialtyOfferingId;
    private DisciplineDTO discipline;
}
