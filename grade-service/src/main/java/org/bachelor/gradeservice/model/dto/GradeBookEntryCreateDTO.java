package org.bachelor.gradeservice.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class GradeBookEntryCreateDTO {
    @NotNull
    private Long specialtyDisciplineId;

    @NotNull
    private Long professorId;

    @NotBlank
    @Pattern(regexp = "\\d{4}/\\d{4}", message = "Формат: 2024/2025")
    private String academicYear;

    @NotEmpty
    private List<Long> bookNumberIds;

    private LocalDate reportDate;

    /** If set, the created entry will have attempt = max(auto, minAttempt). */
    private Integer minAttempt;
}
