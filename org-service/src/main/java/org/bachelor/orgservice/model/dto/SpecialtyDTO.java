package org.bachelor.orgservice.model.dto;

import lombok.Data;
import org.bachelor.orgservice.model.entity.Degree;
import org.bachelor.orgservice.model.entity.EduType;

import java.time.Instant;

@Data
public class SpecialtyDTO {

    private Long id;
    private String code;
    private String nameUA;
    private String nameEN;
    private String studyProgramUA;
    private String studyProgramEN;
    private String eduProgramUA;
    private String eduProgramEN;
    private Long orgId;
    private Degree degree;
    private EduType eduType;
    private Instant startDate;
    private Instant endDate;
}
