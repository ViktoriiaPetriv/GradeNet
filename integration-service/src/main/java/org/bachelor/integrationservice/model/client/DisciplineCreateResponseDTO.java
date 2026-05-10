package org.bachelor.integrationservice.model.client;

import lombok.Data;

@Data
public class DisciplineCreateResponseDTO {
    private Long disciplineId;
    private String name;
    private Long specialtyDisciplineId;
    private Long specialtyId;
}
