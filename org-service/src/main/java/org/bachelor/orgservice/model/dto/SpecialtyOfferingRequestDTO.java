package org.bachelor.orgservice.model.dto;

import jakarta.validation.constraints.NotNull;

public record SpecialtyOfferingRequestDTO(
        @NotNull(message = "Specialty ID is required")
        Long specialtyId,

        Long externalId,

        @NotNull(message = "Graduation year is required")
        Integer graduationYear
) {}
