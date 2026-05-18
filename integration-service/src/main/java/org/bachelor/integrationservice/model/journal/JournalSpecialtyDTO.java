package org.bachelor.integrationservice.model.journal;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class JournalSpecialtyDTO {

    @JsonAlias("external_id")
    private Long externalId;

    private String name;

    private String code;

    private String degree;

    @JsonAlias("graduation_year")
    private Integer graduationYear;

    @JsonAlias("study_form")
    private String studyForm;
}
