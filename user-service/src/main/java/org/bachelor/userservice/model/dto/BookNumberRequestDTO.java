package org.bachelor.userservice.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


public record BookNumberRequestDTO(
        @NotBlank(message = "Number is required")
        @Size(max = 20, message = "Number must be at most 20 characters")
        String number,

        @NotNull(message = "Student is required")
        Long studentId,

        Long specialtyOfferingId
) {}
