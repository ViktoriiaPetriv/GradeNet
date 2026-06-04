package org.bachelor.userservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.bachelor.userservice.exception.NotFoundException;
import org.bachelor.userservice.exception.ValidationException;
import org.bachelor.userservice.mapper.UserMapper;
import org.bachelor.userservice.model.dto.AdminSetupRequestDTO;
import org.bachelor.userservice.model.dto.AuthenticatedUser;
import org.bachelor.userservice.model.dto.ChangePasswordRequestDTO;
import org.bachelor.userservice.model.dto.ImportStudentRequestDTO;
import org.bachelor.userservice.model.dto.StudentInfoDTO;
import org.bachelor.userservice.model.dto.UserDTO;
import org.bachelor.userservice.model.dto.UserProfileDTO;
import org.bachelor.userservice.model.dto.UserRequestDTO;
import org.bachelor.userservice.model.entity.BookNumber;
import org.bachelor.userservice.model.entity.Role;
import org.bachelor.userservice.model.entity.User;
import org.bachelor.userservice.model.entity.UserOrganization;
import org.bachelor.userservice.repository.BookNumberRepository;
import org.bachelor.userservice.repository.UserOrganizationRepository;
import org.bachelor.userservice.repository.UserRepository;
import org.bachelor.userservice.service.AccessControlService;
import org.bachelor.userservice.service.UserService;
import org.bachelor.userservice.utils.SecurityUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final BookNumberRepository bookNumberRepository;
    private final UserOrganizationRepository userOrganizationRepository;
    private final AccessControlService accessControlService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;


    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> findAll(Role role) {
        AuthenticatedUser currentUser = SecurityUtils.getCurrentUser();

        if (currentUser.isAdmin()) {
            return userRepository.findAll().stream()
                    .filter(u -> role == null || u.getRole() == role)
                    .map(userMapper::toDto)
                    .toList();
        }

        if (currentUser.isManager()) {
            Set<Long> allIds = accessControlService.getAllowedUserIdsForManager();
            if (allIds.isEmpty()) return List.of();
            return userRepository.findAllById(allIds).stream()
                    .filter(u -> role == null || u.getRole() == role)
                    .map(userMapper::toDto)
                    .toList();
        }

        throw new AccessDeniedException("Недостатньо прав");
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO findById(Long id) {
        accessControlService.requireAdminOrManagerOfUser(id);
        return userMapper.toDto(getUserOrThrow(id));
    }

    @Override
    @Transactional
    public UserDTO create(UserRequestDTO request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new ValidationException("Користувач з такою електронною поштою вже існує: %s".formatted(request.email()));
        }

        validatePassword(request.password());
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
        AuthenticatedUser currentUser = SecurityUtils.getCurrentUser();

        if (!currentUser.isAdmin()) {
            throw new AccessDeniedException("Недостатньо прав для редагування профілю");
        }

        User user = getUserOrThrow(id);

        if (!user.getEmail().equals(request.email())
                && userRepository.findByEmail(request.email()).isPresent()) {
            throw new ValidationException("Користувач з такою електронною поштою вже існує: %s"
                    .formatted(request.email()));
        }

        validateFieldsByRole(request);
        userMapper.updateEntity(request, user);

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
        if (bookNumberRepository.existsByStudentId(id)) {
            throw new ValidationException(
                "Неможливо видалити користувача: спочатку видаліть його залікові книжки");
        }
        userRepository.delete(user);
    }

    private User getUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Користувача з ID %s не знайдено".formatted(id)));
    }


    private void validateFieldsByRole(UserRequestDTO request) {
        Role role = request.role();

        if (role == Role.MANAGER && request.orgId() == null) {
            throw new ValidationException("Для користувача з роллю менеджера обов'язково потрібно вказати факультет");
        }

        if (role == Role.PROFESSOR || role == Role.STUDENT) {
            if (isNullOrBlank(request.firstName()) || isNullOrBlank(request.lastName())) {
                throw new ValidationException("Ім’я та прізвище є обов’язковими для ролі %s".formatted(role));
            }
        }

        if (role == Role.STUDENT && request.birthDate() == null) {
            throw new ValidationException("Дата народження є обов'язковою для студента");
        }
    }

    private boolean isNullOrBlank(String str) {
        return str == null || str.isBlank();
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileDTO getProfile(Long id) {
        accessControlService.requireAdminOrManagerOfUser(id);

        User user = getUserOrThrow(id);
        UserProfileDTO profile = userMapper.toProfileDto(user);

        if (user.getRole() != Role.STUDENT) {
            return profile;
        }

        Long orgId = user.getOrganizations().stream()
                .findFirst()
                .map(UserOrganization::getOrgId)
                .orElse(null);

        List<StudentInfoDTO> books = bookNumberRepository.findAllByStudentId(id)
                .stream()
                .map(book -> new StudentInfoDTO(
                        book.getId(),
                        book.getNumber(),
                        book.getStatus().name(),
                        book.getRegStartDate(),
                        book.getRegEndDate(),
                        book.getSpecialtyOfferingId(),
                        orgId
                ))
                .toList();

        return profile.withBooks(books);
    }

    private void validatePassword(String password) {
        if (password == null || password.isBlank()) return;
        if (!password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$")) {
            throw new ValidationException("Пароль має містити мінімум 8 символів, велику і малу літеру, цифру та спецсимвол (@$!%*?&)");
        }
    }

    @Override
    @Transactional
    public void changePassword(Long id, ChangePasswordRequestDTO request) {
        AuthenticatedUser currentUser = SecurityUtils.getCurrentUser();

        if (!currentUser.isAdmin() && !currentUser.getUserId().equals(id)) {
            throw new AccessDeniedException("Ви можете змінювати тільки свій пароль");
        }

        if (currentUser.isAdmin() && !currentUser.getUserId().equals(id)) {
            User target = getUserOrThrow(id);
            if (target.getRole() == Role.ADMIN) {
                throw new AccessDeniedException("Адміністратор не може змінювати пароль іншому адміністратору");
            }
        }

        User user = getUserOrThrow(id);
        validatePassword(request.newPassword());
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public UserDTO createStudentFromImport(ImportStudentRequestDTO request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            return userMapper.toDto(userRepository.findByEmail(request.email()).get());
        }
        User user = new User();
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPatronymic(request.patronymic());
        user.setEmail(request.email());
        user.setRole(Role.STUDENT);
        // Generate a temporary password the student can change later
        String tempPassword = "Tmp@" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        user.setPassword(passwordEncoder.encode(tempPassword));
        return userMapper.toDto(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSetupRequired() {
        return !userRepository.existsByRole(Role.ADMIN);
    }

    @Override
    @Transactional
    public UserDTO createInitialAdmin(AdminSetupRequestDTO request) {
        if (!isSetupRequired()) {
            throw new ValidationException("Адміністратор вже існує в системі");
        }

        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new ValidationException("Користувач з такою електронною поштою вже існує: %s".formatted(request.email()));
        }

        validatePassword(request.password());

        User admin = new User();
        admin.setEmail(request.email());
        admin.setPassword(passwordEncoder.encode(request.password()));
        admin.setRole(Role.ADMIN);

        return userMapper.toDto(userRepository.save(admin));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> findStudentsBySpecialty(Long specialtyOfferingId, Integer enrollYear) {
        AuthenticatedUser currentUser = SecurityUtils.getCurrentUser();

        if (!currentUser.isAdmin() && !currentUser.isManager()) {
            throw new AccessDeniedException("Недостатньо прав");
        }

        List<BookNumber> books = enrollYear != null
                ? bookNumberRepository.findAllBySpecialtyOfferingIdAndEnrollYear(specialtyOfferingId, enrollYear)
                : bookNumberRepository.findAllBySpecialtyOfferingId(specialtyOfferingId);

        List<Long> studentIds = books.stream()
                .map(b -> b.getStudent().getId())
                .distinct()
                .toList();

        if (studentIds.isEmpty()) return List.of();

        return userRepository.findAllById(studentIds)
                .stream()
                .map(userMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDTO> searchStudents(String query) {
        AuthenticatedUser currentUser = SecurityUtils.getCurrentUser();

        if (!currentUser.isAdmin() && !currentUser.isManager()) {
            throw new AccessDeniedException("Недостатньо прав");
        }

        return userRepository.search(query)
                .stream()
                .map(userMapper::toDto)
                .toList();
    }
}
