package org.bachelor.userservice.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StudentGroupRequestDTO(
        @NotBlank(message = "Name is required")
        @Size(max = 20, message = "Name must be at most 20 characters")
        String name
) {}
