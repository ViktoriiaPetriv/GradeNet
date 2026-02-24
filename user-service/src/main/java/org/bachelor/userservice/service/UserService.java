package org.bachelor.userservice.service;

import org.bachelor.userservice.model.dto.UserDTO;
import org.bachelor.userservice.model.dto.UserProfileDTO;
import org.bachelor.userservice.model.dto.UserRequestDTO;

import java.util.List;

public interface UserService {
    List<UserDTO> findAll();
    UserDTO findById(Long id);
    UserDTO create(UserRequestDTO request);
    UserDTO update(Long id, UserRequestDTO request);
    void delete(Long id);
    UserProfileDTO getProfile(Long id);
}
