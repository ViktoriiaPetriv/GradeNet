package org.bachelor.gradeservice.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DisciplineCreateDTO {
    @NotBlank
    @Size(max = 255)
    private String name;

    @NotNull
    private Long specialtyOfferingId;

    @NotNull
    @Valid
    private HoursCreateDTO hours;
}
