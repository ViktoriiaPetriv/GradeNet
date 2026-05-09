package org.bachelor.gradeservice.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class HoursCreateDTO {
    @NotBlank
    @Pattern(regexp = "\\d{4}/\\d{4}", message = "Формат: 2024/2025")
    private String academicYear;

    @NotNull
    @Min(1)
    private Integer ectsCredits;

    @NotNull
    @Min(1)
    private Integer totalHours;

    @NotNull
    @Min(0)
    private Integer classroomHours;

    @Min(0)
    private Integer lectureHours = 0;

    @Min(0)
    private Integer seminarHours = 0;

    @Min(0)
    private Integer laboratoryHours = 0;

    @Min(0)
    private Integer individualHours = 0;

    @Min(0)
    private Integer selfWorkHours = 0;
}
