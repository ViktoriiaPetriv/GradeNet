package org.bachelor.userservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.bachelor.userservice.exception.NotFoundException;
import org.bachelor.userservice.exception.ValidationException;
import org.bachelor.userservice.mapper.UserMapper;
import org.bachelor.userservice.model.dto.UserDTO;
import org.bachelor.userservice.model.dto.UserRequestDTO;
import org.bachelor.userservice.model.entity.Role;
import org.bachelor.userservice.model.entity.User;
import org.bachelor.userservice.model.entity.UserOrganization;
import org.bachelor.userservice.repository.UserOrganizationRepository;
import org.bachelor.userservice.repository.UserRepository;
import org.bachelor.userservice.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserOrganizationRepository userOrganizationRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;


    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> findAll() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO findById(Long id) {
        return userMapper.toDto(getUserOrThrow(id));
    }

    @Override
    @Transactional
    public UserDTO create(UserRequestDTO request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new ValidationException("Email already in use: " + request.email());
        }

        validateFieldsByRole(request);

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.password()));
        User savedUser = userRepository.save(user);

        if (request.role() == Role.MANAGER) {
            UserOrganization userOrg = new UserOrganization();
            userOrg.setUser(savedUser);
            userOrg.setOrgId(request.orgId());
            userOrganizationRepository.save(userOrg);
        }

        return userMapper.toDto(savedUser);
    }

    @Override
    @Transactional
    public UserDTO update(Long id, UserRequestDTO request) {
        User user = getUserOrThrow(id);

        if (!user.getEmail().equals(request.email())
                && userRepository.findByEmail(request.email()).isPresent()) {
            throw new ValidationException("Email already in use: " + request.email());
        }

        validateFieldsByRole(request);

        userMapper.updateEntity(request, user);

        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        if (request.role() == Role.MANAGER && request.orgId() != null) {
            updateManagerOrganization(user, request.orgId());
        }

        return userMapper.toDto(userRepository.save(user));
    }

    private void updateManagerOrganization(User user, Long newOrgId) {
        userOrganizationRepository.findByUser(user).ifPresentOrElse(
                org -> org.setOrgId(newOrgId),
                () -> saveOrgLink(user, newOrgId)
        );
    }

    private void saveOrgLink(User user, Long orgId) {
        UserOrganization newOrg = new UserOrganization();
        newOrg.setUser(user);
        newOrg.setOrgId(orgId);
        userOrganizationRepository.save(newOrg);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        User user = getUserOrThrow(id);
        userRepository.delete(user);
    }

    private User getUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User with %s not found".formatted(id)));
    }


    private void validateFieldsByRole(UserRequestDTO request) {
        Role role = request.role();

        if (role == Role.MANAGER && request.orgId() == null) {
            throw new ValidationException("Organization ID is required for Manager");
        }

        if (role == Role.PROFESSOR || role == Role.STUDENT) {
            if (isNullOrBlank(request.firstName()) || isNullOrBlank(request.lastName())) {
                throw new ValidationException("First and Last name are required for " + role);
            }
        }

        if (role == Role.STUDENT && request.birthDate() == null) {
            throw new ValidationException("Birth date is required for Student");
        }
    }

    private boolean isNullOrBlank(String str) {
        return str == null || str.isBlank();
    }
}
