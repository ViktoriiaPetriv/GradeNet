package org.bachelor.integrationservice.model.client;

import lombok.Data;

@Data
public class GradeBookEntryDTO {
    private Long id;
    private Long bookNumberId;
    private Long specialtyDisciplineId;
    private Long professorId;
    private String academicYear;
    private Integer attempt;
    private String status;
    private String result;
    private Integer semester;
}
