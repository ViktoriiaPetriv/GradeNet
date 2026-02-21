package org.bachelor.userservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bachelor.userservice.model.dto.UserDTO;
import org.bachelor.userservice.model.dto.UserRequestDTO;
import org.bachelor.userservice.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @GetMapping
    public List<UserDTO> findAll() {
        return userService.findAll();
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
}
