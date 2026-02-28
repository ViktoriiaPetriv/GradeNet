package org.bachelor.userservice.model.dto;

import org.bachelor.userservice.model.entity.Role;

import java.time.LocalDate;
import java.util.List;

public record UserProfileDTO(
        Long id,
        String firstName,
        String lastName,
        String patronymic,
        String email,
        LocalDate birthDate,
        Role role,
        List<StudentInfoDTO> books
) {
    public UserProfileDTO withBooks(List<StudentInfoDTO> books) {
        return new UserProfileDTO(id, firstName, lastName, patronymic, email, birthDate, role, books);
    }
}
