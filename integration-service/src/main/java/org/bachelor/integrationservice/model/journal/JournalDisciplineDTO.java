package org.bachelor.integrationservice.model.journal;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class JournalDisciplineDTO {

    @JsonAlias("external_id")
    private Long externalId;

    private String name;
}
