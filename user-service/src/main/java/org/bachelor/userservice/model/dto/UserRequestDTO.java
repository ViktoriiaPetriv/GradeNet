package org.bachelor.userservice.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import org.bachelor.userservice.model.entity.Role;

import java.time.LocalDate;

public record UserRequestDTO (
        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must be at most 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must be at most 100 characters")
        String lastName,

        @Size(max = 100, message = "Patronymic must be at most 100 characters")
        String patronymic,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 255, message = "Password must be between 6 and 255 characters")
        String password,

        @NotNull(message = "Birth date is required")
        @Past(message = "Birth date must be in the past")
        LocalDate birthDate,

        @NotNull(message = "Role is required")
        Role role
) {}
