package org.bachelor.integrationservice.model.journal;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.util.List;

@Data
public class JournalDisciplineDetailDTO {

    @JsonAlias("external_id")
    private Long externalId;

    private String name;

    @JsonAlias("academic_year")
    private String academicYear;

    private Integer semester;

    @JsonAlias("ects_credits")
    private Integer ectsCredits;

    @JsonAlias("total_hours")
    private Integer totalHours;

    private List<JournalStudentGradeDTO> grades;
}
