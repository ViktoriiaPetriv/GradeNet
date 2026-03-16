package org.bachelor.userservice.model.dto;

import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequestDTO(
        @NotBlank(message = "Новий пароль є обов'язковим")
        String newPassword
) {}