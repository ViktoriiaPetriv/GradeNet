package org.bachelor.gradeservice.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DisciplineUpdateDTO {

    @NotBlank
    @Size(max = 255)
    private String name;
}
