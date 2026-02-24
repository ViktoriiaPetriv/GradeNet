package org.bachelor.userservice.model.dto;

import org.bachelor.userservice.model.entity.Role;

import java.time.LocalDate;

public record UserProfileDTO(
        Long id,
        String firstName,
        String lastName,
        String patronymic,
        String email,
        LocalDate birthDate,
        Role role,
        StudentInfoDTO studentInfo
) {
    public UserProfileDTO withStudentInfo(StudentInfoDTO studentInfo) {
        return new UserProfileDTO(id, firstName, lastName, patronymic, email, birthDate, role, studentInfo);
    }
}
