package org.bachelor.integrationservice.model.journal;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class JournalStudentGradeDTO {

    @JsonAlias("student_external_id")
    private Long studentExternalId;

    @JsonAlias("university_grade")
    private Integer universityGrade;

    private Integer attempt;

    @JsonAlias("assessment_type")
    private Integer assessmentType;

    @JsonAlias("assessment_date")
    private String assessmentDate;
}
