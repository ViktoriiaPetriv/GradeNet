package org.bachelor.gradeservice.model.dto;

public record UserResponse(
        Long id,
        String email,
        String role,
        String firstName,
        String lastName,
        String patronymic
) {}
