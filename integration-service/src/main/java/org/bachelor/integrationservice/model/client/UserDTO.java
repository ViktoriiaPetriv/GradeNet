package org.bachelor.integrationservice.model.client;

import lombok.Data;

@Data
public class UserDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String patronymic;
    private String email;
    private String role;
}
