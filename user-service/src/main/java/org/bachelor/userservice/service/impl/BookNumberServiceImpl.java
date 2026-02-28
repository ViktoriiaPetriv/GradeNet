package org.bachelor.userservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.bachelor.userservice.exception.NotFoundException;
import org.bachelor.userservice.exception.ValidationException;
import org.bachelor.userservice.mapper.BookNumberMapper;
import org.bachelor.userservice.model.dto.BookNumberDTO;
import org.bachelor.userservice.model.dto.BookNumberRequestDTO;
import org.bachelor.userservice.model.dto.PageResponse;
import org.bachelor.userservice.model.entity.BookNumber;
import org.bachelor.userservice.model.entity.BookNumberStatus;
import org.bachelor.userservice.model.entity.Role;
import org.bachelor.userservice.model.entity.User;
import org.bachelor.userservice.repository.BookNumberRepository;
import org.bachelor.userservice.repository.BookNumberSpecification;
import org.bachelor.userservice.repository.UserRepository;
import org.bachelor.userservice.service.BookNumberService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookNumberServiceImpl implements BookNumberService {

    private final BookNumberRepository bookNumberRepository;
    private final BookNumberMapper bookNumberMapper;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public PageResponse<BookNumberDTO> findAll(String number, Pageable pageable) {
        Specification<BookNumber> spec = BookNumberSpecification.numberContains(number);
        return PageResponse.of(
                bookNumberRepository.findAll(spec, pageable)
                        .map(bookNumberMapper::toDto)
        );
    }

    @Override
    @Transactional
    public BookNumberDTO findById(Long id) {
        return bookNumberMapper.toDto(getBookOrThrow(id));
    }

    @Override
    @Transactional
    public List<BookNumberDTO> findByStudentId(Long studentId) {
        return bookNumberRepository.findAllByStudentId(studentId)
                .stream()
                .map(bookNumberMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public BookNumberDTO create(BookNumberRequestDTO request) {
        if (bookNumberRepository.existsByNumber(request.number())) {
            throw new ValidationException("Номер залікової книжки вже існує: %s".formatted(request.number()));
        }
        User student = userRepository.findById(request.studentId())
                .orElseThrow(() -> new NotFoundException("Користувача з ID %s не знайдено".formatted(request.studentId())));

        if (student.getRole() != Role.STUDENT) {
            throw new ValidationException("Користувач з ID %s не є студентом".formatted(request.studentId()));
        }

        if (bookNumberRepository.existsByStudentIdAndSpecialtyId(request.studentId(), request.specialtyId())) {
            throw new ValidationException(
                    "У цього студента вже є залікова книжка для цієї спеціальності"
            );
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
        bookNumberRepository.delete(bookNumber);
    }

    @Override
    @Transactional
    public BookNumberDTO markAsFilled(Long id) {
        BookNumber bookNumber = getBookOrThrow(id);

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
}
