package org.bachelor.userservice.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bachelor.userservice.model.dto.AdminSetupRequestDTO;
import org.bachelor.userservice.model.dto.AuthResponseDTO;
import org.bachelor.userservice.model.dto.LoginRequestDTO;
import org.bachelor.userservice.model.dto.UserDTO;
import org.bachelor.userservice.service.AuthService;
import org.bachelor.userservice.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @GetMapping("/setup")
    public Map<String, Boolean> checkSetup() {
        return Map.of("setupRequired", userService.isSetupRequired());
    }

    @PostMapping("/setup")
    public UserDTO setupAdmin(@Valid @RequestBody AdminSetupRequestDTO request) {
        return userService.createInitialAdmin(request);
    }

    @PostMapping("/login")
    public AuthResponseDTO login(@Valid @RequestBody LoginRequestDTO request,
                                 HttpServletResponse response) {
        return authService.login(request, response);
    }

    @PostMapping("/refresh")
    public AuthResponseDTO refresh(HttpServletRequest request,
                                   HttpServletResponse response) {
        return authService.refresh(request, response);
    }

    @PostMapping("/logout")
    public void logout(HttpServletResponse response) {
        authService.logout(response);
    }
}