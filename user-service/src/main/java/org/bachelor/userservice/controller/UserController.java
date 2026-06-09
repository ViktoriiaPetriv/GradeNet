package org.bachelor.userservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bachelor.userservice.model.dto.ChangePasswordRequestDTO;
import org.bachelor.userservice.model.dto.ImportStudentRequestDTO;
import org.bachelor.userservice.model.dto.SelfUpdateRequestDTO;
import org.bachelor.userservice.model.dto.UserDTO;
import org.bachelor.userservice.model.dto.UserProfileDTO;
import org.bachelor.userservice.model.dto.UserRequestDTO;
import org.bachelor.userservice.service.UserService;
import org.springframework.web.bind.annotation.*;

import org.bachelor.userservice.model.entity.Role;
import org.bachelor.userservice.utils.SecurityUtils;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @GetMapping
    public List<UserDTO> findAll(@RequestParam(required = false) String role) {
        Role roleEnum = (role != null && !role.isBlank()) ? Role.valueOf(role) : null;
        return userService.findAll(roleEnum);
    }

    @GetMapping("/{id}")
    public UserDTO findById(@PathVariable Long id) {
        return userService.findById(id);
    }

    @PostMapping
    public UserDTO create(@Valid @RequestBody UserRequestDTO request) {
        return userService.create(request);
    }

    @PutMapping("/{id}")
    public UserDTO update(
            @PathVariable Long id,
            @Valid @RequestBody UserRequestDTO request) {
        return userService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        userService.delete(id);
    }

    @GetMapping("/me/profile")
    public UserProfileDTO getMyProfile() {
        return userService.getProfile(SecurityUtils.getCurrentUserId());
    }

    @GetMapping("/{id}/profile")
    public UserProfileDTO getProfile(@PathVariable Long id) {
        return userService.getProfile(id);
    }

    @PatchMapping("/{id}/self")
    public UserDTO updateSelf(@PathVariable Long id, @Valid @RequestBody SelfUpdateRequestDTO request) {
        return userService.updateSelf(id, request);
    }

    @PatchMapping("/{id}/password")
    public void changePassword(@PathVariable Long id, @Valid @RequestBody ChangePasswordRequestDTO request) {
        userService.changePassword(id, request);
    }

    @PostMapping("/import-student")
    public UserDTO createStudentFromImport(@Valid @RequestBody ImportStudentRequestDTO request) {
        return userService.createStudentFromImport(request);
    }

    @GetMapping("/students")
    public List<UserDTO> findStudentsBySpecialty(
            @RequestParam Long specialtyOfferingId,
            @RequestParam(required = false) Integer enrollYear) {
        return userService.findStudentsBySpecialty(specialtyOfferingId, enrollYear);
    }

    @GetMapping("/search")
    public List<UserDTO> search(@RequestParam String query) {
        return userService.searchStudents(query);
    }
}
