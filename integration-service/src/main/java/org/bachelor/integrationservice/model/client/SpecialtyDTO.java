package org.bachelor.integrationservice.model.client;

import lombok.Data;

@Data
public class SpecialtyDTO {
    private Long id;
    private String code;
    private String nameUA;
    private String studyProgramUA;
    private String eduProgramUA;
    private String degree;
}
