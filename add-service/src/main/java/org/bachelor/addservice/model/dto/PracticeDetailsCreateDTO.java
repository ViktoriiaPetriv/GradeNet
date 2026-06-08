package org.bachelor.addservice.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PracticeDetailsCreateDTO {

    @NotBlank
    @Size(max = 255)
    private String organization;

    @NotNull
    private Integer course;

    @NotNull
    private LocalDate startDate;

    private LocalDate endDate;

    private String workDescription;

    @NotNull
    private Integer ectsCredits;

    private Integer totalHours;

    @NotNull
    private Long supervisorId;
}
