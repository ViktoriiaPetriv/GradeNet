package org.bachelor.userservice.model.dto;

import lombok.Data;
import org.bachelor.userservice.model.entity.Role;

import java.time.LocalDate;

@Data
public class UserDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String patronymic;
    private String email;
    private LocalDate birthDate;
    private Role role;
}
