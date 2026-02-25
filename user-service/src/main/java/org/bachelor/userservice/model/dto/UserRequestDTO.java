package org.bachelor.userservice.model.dto;

import jakarta.validation.constraints.*;
import org.bachelor.userservice.model.entity.Role;

import java.time.LocalDate;

public record UserRequestDTO (
        @Size(max = 50, message = "Ім'я не може перевищувати 50 символів")
        @Pattern(
                regexp = "^[\\p{L}'\\- ]*$",
                message = "Ім'я може містити лише літери, апостроф та дефіс"
        )
        String firstName,

        @Size(max = 50, message = "Прізвище не може перевищувати 50 символів")
        @Pattern(
                regexp = "^[\\p{L}'\\- ]*$",
                message = "Прізвище може містити лише літери, апостроф та дефіс"
        )
        String lastName,

        @Size(max = 50, message = "По батькові не може перевищувати 50 символів")
        @Pattern(
                regexp = "^[\\p{L}'\\- ]*$",
                message = "По батькові може містити лише літери, апостроф та дефіс"
        )
        String patronymic,

        @NotBlank(message = "Електронна пошта (email) є обов'язковою")
        @Email(message = "Електронна пошта (email) має бути коректною")
        @Pattern(
                regexp = "^[a-zA-Z0-9._%+\\-]+@(pnu|cnu)\\.edu\\.ua$",
                message = "Електронна пошта (email) має бути у форматі @pnu.edu.ua або @cnu.edu.ua"
        )
        String email,

        @Size(min = 8, max = 32, message = "Пароль має бути від 8 до 32 символів")
        String password,

        @Past(message = "Дата народження має бути в минулому")
        LocalDate birthDate,

        @NotNull(message = "Роль є обов'язковою")
        Role role,

        Long orgId
) {}
