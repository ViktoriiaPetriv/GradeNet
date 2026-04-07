package org.bachelor.gradeservice.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record DisciplineRequestDTO(
        @NotBlank(message = "Name is mandatory") String name,
        @NotNull(message = "Specialty is mandatory") Long specialtyId,
        Long professorId,
        Instant reportDate
) {}
