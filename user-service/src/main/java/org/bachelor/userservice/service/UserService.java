package org.bachelor.userservice.service;

import org.bachelor.userservice.model.dto.AdminSetupRequestDTO;
import org.bachelor.userservice.model.dto.ChangePasswordRequestDTO;
import org.bachelor.userservice.model.dto.ImportStudentRequestDTO;
import org.bachelor.userservice.model.dto.SelfUpdateRequestDTO;
import org.bachelor.userservice.model.dto.UserDTO;
import org.bachelor.userservice.model.dto.UserProfileDTO;
import org.bachelor.userservice.model.dto.UserRequestDTO;
import org.bachelor.userservice.model.entity.Role;

import java.util.List;

public interface UserService {
    List<UserDTO> findAll(Role role);
    UserDTO findById(Long id);
    UserDTO create(UserRequestDTO request);
    UserDTO update(Long id, UserRequestDTO request);
    UserDTO updateSelf(Long id, SelfUpdateRequestDTO request);
    void delete(Long id);
    UserProfileDTO getProfile(Long id);
    void changePassword(Long id, ChangePasswordRequestDTO request);
    List<UserDTO> findStudentsBySpecialty(Long specialtyOfferingId, Integer enrollYear);
    List<UserDTO> searchStudents(String query);
    boolean isSetupRequired();
    UserDTO createInitialAdmin(AdminSetupRequestDTO request);
    UserDTO createStudentFromImport(ImportStudentRequestDTO request);
}
