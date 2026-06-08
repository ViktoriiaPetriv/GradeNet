package org.bachelor.addservice.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CourseWorkDetailsCreateDTO {

    @NotNull
    private Integer semester;

    private String state = "IN_PROGRESS";

    private Integer ectsCredits;

    private Integer totalHours;
}
