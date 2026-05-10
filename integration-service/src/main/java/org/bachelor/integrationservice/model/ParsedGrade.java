package org.bachelor.integrationservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ParsedGrade {
    private int disciplineIndex;
    private Integer universityGrade;
    private String ectsGrade;
    private Object nationalGrade;
}
