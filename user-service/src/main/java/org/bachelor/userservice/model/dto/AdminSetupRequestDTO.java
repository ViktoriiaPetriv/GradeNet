package org.bachelor.userservice.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminSetupRequestDTO(
        @NotBlank(message = "Електронна пошта (email) є обов'язковою")
        @Email(message = "Електронна пошта (email) має бути коректною")
        @Pattern(
                regexp = "^[a-zA-Z0-9._%+\\-]+@(pnu|cnu)\\.edu\\.ua$",
                message = "Електронна пошта (email) має бути у форматі @pnu.edu.ua або @cnu.edu.ua"
        )
        String email,

        @NotBlank(message = "Пароль є обов'язковим")
        @Size(min = 8, max = 32, message = "Пароль має бути від 8 до 32 символів")
        String password
) {}
