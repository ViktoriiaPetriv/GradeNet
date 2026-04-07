package org.bachelor.gradeservice.model.dto;

public record UserDTO(
        Long id,
        String email,
        String role
) {}
