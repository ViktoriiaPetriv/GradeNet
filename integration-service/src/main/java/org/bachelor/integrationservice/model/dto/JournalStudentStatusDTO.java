package org.bachelor.integrationservice.model.dto;

import lombok.Data;

@Data
public class JournalStudentStatusDTO {
    private Long externalId;
    private String firstName;
    private String lastName;
    private String patronymic;
    private String email;
    private boolean existsInSystem;
}
