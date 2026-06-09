package org.bachelor.userservice.model.dto;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record SelfUpdateRequestDTO(
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

        @Past(message = "Дата народження має бути в минулому")
        LocalDate birthDate
) {}
