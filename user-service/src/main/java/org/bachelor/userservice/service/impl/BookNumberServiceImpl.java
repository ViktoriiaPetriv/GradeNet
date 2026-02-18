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
            throw new ValidationException("Book number already exists: " + request.number());
        }
        User student = userRepository.findById(request.studentId())
                .orElseThrow(() -> new NotFoundException("User with %s not found".formatted(request.studentId())));

        if (student.getRole() != Role.STUDENT) {
            throw new ValidationException("User with id %s is not a student".formatted(request.studentId()));
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
            throw new ValidationException("Book number already exists: " + request.number());
        }

        if (request.studentId() != null) {
            User student = userRepository.findById(request.studentId())
                    .orElseThrow(() -> new NotFoundException("User with %s not found".formatted(request.studentId())));

            if (student.getRole() != Role.STUDENT) {
                throw new ValidationException("User with id %s is not a student".formatted(request.studentId()));
            }

            bookNumber.setStudent(student);
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
            throw new ValidationException("Book number must be in REGISTERED status to mark as FILLED");
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
            throw new ValidationException("Book number must be in FILLED status to mark as HANDED");
        }

        bookNumber.setStatus(BookNumberStatus.HANDED);
        bookNumber.setHandedDate(Instant.now());

        return bookNumberMapper.toDto(bookNumberRepository.save(bookNumber));
    }

    private BookNumber getBookOrThrow(Long id) {
        return bookNumberRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Book with %s not found".formatted(id)));
    }
}
