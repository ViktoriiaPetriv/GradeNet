package org.bachelor.userservice.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import org.bachelor.userservice.model.entity.Role;

import java.time.LocalDate;

public record UserRequestDTO (
        @Size(max = 50, message = "Ім'я не може перевищувати 100 символів")
        String firstName,

        @Size(max = 50, message = "Прізвище не може перевищувати 100 символів")
        String lastName,

        @Size(max = 50, message = "По батькові не може перевищувати 100 символів")
        String patronymic,

        @NotBlank(message = "Email є обов'язковим")
        @Email(message = "Email має бути коректним")
        String email,

        @Size(min = 8, max = 32, message = "Пароль має бути від 8 до 32 символів")
        String password,

        @Past(message = "Дата народження має бути в минулому")
        LocalDate birthDate,

        @NotNull(message = "Роль є обов'язковою")
        Role role,

        Long orgId
) {}
