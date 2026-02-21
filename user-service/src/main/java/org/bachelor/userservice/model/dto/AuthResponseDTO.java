package org.bachelor.userservice.model.dto;

public record AuthResponseDTO(
        String token,
        UserDTO user
) {}