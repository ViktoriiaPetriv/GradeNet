package org.bachelor.integrationservice.model.journal;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class JournalStudentDTO {

    // external API has a typo: "externa_id" instead of "external_id"
    @JsonAlias({"externa_id", "external_id"})
    private Long externalId;

    @JsonAlias("reg_start_date")
    private String regStartDate;

    @JsonAlias("first_name")
    private String firstName;

    @JsonAlias("last_name")
    private String lastName;

    private String patronymic;

    private String birth;

    private String email;
}
