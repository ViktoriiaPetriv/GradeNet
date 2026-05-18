package org.bachelor.userservice.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


public record BookNumberRequestDTO(
        @Size(max = 20, message = "Number must be at most 20 characters")
        String number,

        @NotNull(message = "Student is required")
        Long studentId,

        Long specialtyOfferingId
) {}
