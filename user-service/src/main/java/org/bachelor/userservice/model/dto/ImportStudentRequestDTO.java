package org.bachelor.userservice.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ImportStudentRequestDTO(
        @NotBlank
        @Size(max = 50)
        String firstName,

        @NotBlank
        @Size(max = 50)
        String lastName,

        @Size(max = 50)
        String patronymic,

        @NotBlank
        @Email
        String email
) {}
