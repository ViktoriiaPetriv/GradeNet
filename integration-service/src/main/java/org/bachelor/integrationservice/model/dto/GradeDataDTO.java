package org.bachelor.integrationservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GradeDataDTO {
    private int disciplineIndex;
    private Integer universityGrade;
    private String ectsGrade;
    private String nationalGrade;
}
