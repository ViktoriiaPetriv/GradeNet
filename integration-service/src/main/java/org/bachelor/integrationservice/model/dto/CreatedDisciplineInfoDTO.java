package org.bachelor.integrationservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatedDisciplineInfoDTO {
    private int index;
    private Long disciplineId;
    private Long specialtyDisciplineId;
}
