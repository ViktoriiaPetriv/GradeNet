package org.bachelor.addservice.model.dto;

import lombok.Data;

@Data
public class QualificationDetailsDTO {
    private Long id;
    private Long additionalWorkId;
    private Long supervisorId;
    private String state;
}
