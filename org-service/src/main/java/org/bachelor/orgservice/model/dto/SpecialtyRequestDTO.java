package org.bachelor.orgservice.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.bachelor.orgservice.model.entity.Degree;
import org.bachelor.orgservice.model.entity.EduType;

import java.time.Instant;

public record SpecialtyRequestDTO(

        @Size(max = 10, message = "Code cannot be longer than 10 characters")
        @NotBlank(message = "Specialty code is required")
        String code,

        @NotBlank(message = "Specialty name (UA) is required")
        String nameUA,

        @NotBlank(message = "Specialty name (EN) is required")
        String nameEN,

        @NotBlank(message = "Study program name (UA) is required")
        String studyProgramUA,

        @NotBlank(message = "Study program name (EN) is required")
        String studyProgramEN,

        @NotBlank(message = "Educational program name (UA) is required")
        String eduProgramUA,

        @NotBlank(message = "Educational program name (EN) is required")
        String eduProgramEN,

        @NotNull(message = "Organization ID is required")
        Long orgId,

        @NotNull(message = "Degree must be specified")
        Degree degree,

        @NotNull(message = "Education type must be specified")
        EduType eduType,

        @NotNull(message = "Start date is required")
        Instant startDate,

        Instant endDate
) {}
