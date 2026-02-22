package org.bachelor.userservice.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import org.bachelor.userservice.model.entity.Role;

import java.time.LocalDate;

public record UserRequestDTO (
        String firstName,

        String lastName,

        String patronymic,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @Size(min = 6, max = 255, message = "Password must be between 6 and 255 characters")
        String password,

        @Past(message = "Birth date must be in the past")
        LocalDate birthDate,

        @NotNull(message = "Role is required")
        Role role,

        Long orgId
) {}
