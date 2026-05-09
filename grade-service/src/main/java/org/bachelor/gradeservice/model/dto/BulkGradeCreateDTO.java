package org.bachelor.gradeservice.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BulkGradeCreateDTO {
    @NotEmpty
    @Valid
    private List<GradeCreateDTO> grades;
}
