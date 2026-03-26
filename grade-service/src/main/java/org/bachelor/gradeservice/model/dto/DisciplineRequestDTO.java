package org.bachelor.gradeservice.model.dto;

import jakarta.validation.constraints.NotBlank;

public record DisciplineRequestDTO(
        @NotBlank String name
) {}
