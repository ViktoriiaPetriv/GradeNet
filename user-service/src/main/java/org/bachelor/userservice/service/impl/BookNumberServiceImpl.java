package org.bachelor.userservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.bachelor.userservice.exception.NotFoundException;
import org.bachelor.userservice.exception.ValidationException;
import org.bachelor.userservice.mapper.BookNumberMapper;
import org.bachelor.userservice.model.dto.AuthenticatedUser;
import org.bachelor.userservice.model.dto.BookNumberDTO;
import org.bachelor.userservice.model.dto.BookNumberRequestDTO;
import org.bachelor.userservice.model.dto.PageResponse;
import org.bachelor.userservice.model.entity.BookNumber;
import org.bachelor.userservice.model.entity.BookNumberStatus;
import org.bachelor.userservice.model.entity.Role;
import org.bachelor.userservice.model.entity.User;
import org.bachelor.userservice.repository.BookNumberRepository;
import org.bachelor.userservice.repository.UserRepository;
import org.bachelor.userservice.service.AccessControlService;
import org.bachelor.userservice.service.BookNumberService;
import org.bachelor.userservice.utils.SecurityUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BookNumberServiceImpl implements BookNumberService {

    private final BookNumberRepository bookNumberRepository;
    private final BookNumberMapper bookNumberMapper;
    private final UserRepository userRepository;
    private final AccessControlService accessControlService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BookNumberDTO> findAll(String number, Pageable pageable) {
        AuthenticatedUser currentUser = SecurityUtils.getCurrentUser();

        if (currentUser.isAdmin()) {
            Specification<BookNumber> spec = (root, query, cb) -> cb.conjunction();
            return getBookNumberDTOPageResponse(number, pageable, spec);
        }

        if (currentUser.isManager()) {
            List<Long> specialtyIds = accessControlService.getManagerSpecialtyIds();
            return findAllBySpecialtyIds(specialtyIds, number, pageable);
        }

        throw new AccessDeniedException("Недостатньо прав");
    }

    @Override
    @Transactional(readOnly = true)
    public BookNumberDTO findById(Long id) {
        BookNumber bookNumber = getBookOrThrow(id);
        accessControlService.requireAdminOrManagerOfSpecialty(bookNumber.getSpecialtyId());
        return bookNumberMapper.toDto(bookNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookNumberDTO> findByStudentId(Long studentId) {
        AuthenticatedUser currentUser = SecurityUtils.getCurrentUser();

        if (currentUser.isAdmin()) {
            return bookNumberRepository.findAllByStudentId(studentId)
                    .stream().map(bookNumberMapper::toDto).toList();
        }

        if (currentUser.isManager()) {
            // Перевіряємо чи студент належить факультету менеджера
            Set<Long> allowedIds = accessControlService.getAllowedUserIdsForManager();
            if (!allowedIds.contains(studentId)) {
                throw new AccessDeniedException("Немає доступу до цього студента");
            }
            return bookNumberRepository.findAllByStudentId(studentId)
                    .stream().map(bookNumberMapper::toDto).toList();
        }

        // Студент бачить тільки свої
        if (currentUser.getUserId().equals(studentId)) {
            return bookNumberRepository.findAllByStudentId(studentId)
                    .stream().map(bookNumberMapper::toDto).toList();
        }

        throw new AccessDeniedException("Недостатньо прав");
    }

    @Override
    @Transactional
    public BookNumberDTO create(BookNumberRequestDTO request) {
        accessControlService.requireAdminOrManagerOfSpecialty(request.specialtyId());

        if (bookNumberRepository.existsByNumber(request.number())) {
            throw new ValidationException("Номер залікової книжки вже існує: %s".formatted(request.number()));
        }

        User student = userRepository.findById(request.studentId())
                .orElseThrow(() -> new NotFoundException("Користувача з ID %s не знайдено".formatted(request.studentId())));

        if (student.getRole() != Role.STUDENT) {
            throw new ValidationException("Користувач з ID %s не є студентом".formatted(request.studentId()));
        }

        if (bookNumberRepository.existsByStudentIdAndSpecialtyId(request.studentId(), request.specialtyId())) {
            throw new ValidationException("У цього студента вже є залікова книжка для цієї спеціальності");
        }

        BookNumber bookNumber = bookNumberMapper.toEntity(request);
        bookNumber.setStudent(student);
        bookNumber.setStatus(BookNumberStatus.REGISTERED);
        bookNumber.setRegStartDate(Instant.now());

        return bookNumberMapper.toDto(bookNumberRepository.save(bookNumber));
    }

    @Override
    @Transactional
    public BookNumberDTO update(Long id, BookNumberRequestDTO request) {
        BookNumber bookNumber = getBookOrThrow(id);
        accessControlService.requireAdminOrManagerOfSpecialty(bookNumber.getSpecialtyId());

        if (request.number() != null
                && !bookNumber.getNumber().equals(request.number())
                && bookNumberRepository.existsByNumber(request.number())) {
            throw new ValidationException("Номер залікової книжки вже існує: %s".formatted(request.number()));
        }

        if (request.studentId() != null && !bookNumber.getStudent().getId().equals(request.studentId())) {
            User student = userRepository.findById(request.studentId())
                    .orElseThrow(() -> new NotFoundException("Користувача з ID %s не знайдено".formatted(request.studentId())));

            if (student.getRole() != Role.STUDENT) {
                throw new ValidationException("Користувач з ID %s не є студентом".formatted(request.studentId()));
            }

            Long specialtyId = request.specialtyId() != null ? request.specialtyId() : bookNumber.getSpecialtyId();
            if (bookNumberRepository.existsByStudentIdAndSpecialtyId(student.getId(), specialtyId)
                    && !(student.getId().equals(bookNumber.getStudent().getId())
                    && specialtyId.equals(bookNumber.getSpecialtyId()))) {
                throw new ValidationException("У цього студента вже є залікова книжка для цієї спеціальності");
            }

            bookNumber.setStudent(student);
        }

        if (request.specialtyId() != null && !request.specialtyId().equals(bookNumber.getSpecialtyId())) {
            Long studentId = request.studentId() != null ? request.studentId() : bookNumber.getStudent().getId();
            if (bookNumberRepository.existsByStudentIdAndSpecialtyId(studentId, request.specialtyId())) {
                throw new ValidationException("У цього студента вже є залікова книжка для цієї спеціальності");
            }
            bookNumber.setSpecialtyId(request.specialtyId());
        }

        bookNumberMapper.updateEntity(request, bookNumber);
        return bookNumberMapper.toDto(bookNumberRepository.save(bookNumber));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        BookNumber bookNumber = getBookOrThrow(id);
        accessControlService.requireAdminOrManagerOfSpecialty(bookNumber.getSpecialtyId());
        bookNumberRepository.delete(bookNumber);
    }

    @Override
    @Transactional
    public BookNumberDTO markAsFilled(Long id) {
        BookNumber bookNumber = getBookOrThrow(id);
        accessControlService.requireAdminOrManagerOfSpecialty(bookNumber.getSpecialtyId());

        if (bookNumber.getStatus() != BookNumberStatus.REGISTERED) {
            throw new ValidationException("Залікову книжку можна позначити заповненою лише після її реєстрації");
        }

        bookNumber.setStatus(BookNumberStatus.FILLED);
        bookNumber.setRegEndDate(Instant.now());
        return bookNumberMapper.toDto(bookNumberRepository.save(bookNumber));
    }

    @Override
    @Transactional
    public BookNumberDTO markAsHanded(Long id) {
        BookNumber bookNumber = getBookOrThrow(id);
        accessControlService.requireAdminOrManagerOfSpecialty(bookNumber.getSpecialtyId());

        if (bookNumber.getStatus() != BookNumberStatus.FILLED) {
            throw new ValidationException("Залікову книжку можна видати студенту лише після її заповнення");
        }

        bookNumber.setStatus(BookNumberStatus.HANDED);
        bookNumber.setHandedDate(Instant.now());
        return bookNumberMapper.toDto(bookNumberRepository.save(bookNumber));
    }

    private BookNumber getBookOrThrow(Long id) {
        return bookNumberRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Залікову книжку з ID %s не знайдено".formatted(id)));
    }

    private PageResponse<BookNumberDTO> findAllBySpecialtyIds(
            List<Long> specialtyIds, String number, Pageable pageable) {
        if (specialtyIds.isEmpty()) return PageResponse.empty();

        Specification<BookNumber> spec = (root, query, cb) ->
                root.get("specialtyId").in(specialtyIds);

        return getBookNumberDTOPageResponse(number, pageable, spec);
    }

    private PageResponse<BookNumberDTO> getBookNumberDTOPageResponse(
            String number, Pageable pageable, Specification<BookNumber> spec) {
        if (number != null && !number.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("number")), "%" + number.toLowerCase() + "%"));
        }
        return PageResponse.of(bookNumberRepository.findAll(spec, pageable).map(bookNumberMapper::toDto));
    }
}