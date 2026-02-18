package org.bachelor.userservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.bachelor.userservice.model.dto.BookNumberDTO;
import org.bachelor.userservice.model.dto.BookNumberRequestDTO;
import org.bachelor.userservice.model.dto.PageResponse;
import org.bachelor.userservice.service.BookNumberService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookNumberController {

    private final BookNumberService bookNumberService;

    @GetMapping
    public PageResponse<BookNumberDTO> findAll(
            @RequestParam(required = false) String number,
            @RequestParam(defaultValue = "0") int pageNumber,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Pageable pageable = PageRequest.of(
                pageNumber, size,
                sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending()
        );

        return bookNumberService.findAll(number, pageable);
    }

    @GetMapping("/{id}")
    public BookNumberDTO findById(@PathVariable Long id) {
        return bookNumberService.findById(id);
    }

    @GetMapping("/student/{studentId}")
    public List<BookNumberDTO> findByStudentId(@PathVariable Long studentId) {
        return bookNumberService.findByStudentId(studentId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookNumberDTO create(@Valid @RequestBody BookNumberRequestDTO request) {
        return bookNumberService.create(request);
    }

    @PutMapping("/{id}")
    public BookNumberDTO update(
            @PathVariable Long id,
            @Valid @RequestBody BookNumberRequestDTO request) {
        return bookNumberService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        bookNumberService.delete(id);
    }

    @PatchMapping("/{id}/fill")
    public BookNumberDTO markAsFilled(@PathVariable Long id) {
        return bookNumberService.markAsFilled(id);
    }

    @PatchMapping("/{id}/hand")
    public BookNumberDTO markAsHanded(@PathVariable Long id) {
        return bookNumberService.markAsHanded(id);
    }
}
