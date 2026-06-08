package org.bachelor.addservice.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class QualificationDetailsCreateDTO {

    @NotNull
    private Long supervisorId;

    private String state = "IN_PROGRESS";
}
