package org.bachelor.userservice.service;

import org.bachelor.userservice.model.dto.BookNumberDTO;
import org.bachelor.userservice.model.dto.BookNumberRequestDTO;
import org.bachelor.userservice.model.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BookNumberService {
    PageResponse<BookNumberDTO> findAll(String number, Pageable pageable);
    BookNumberDTO findById(Long id);
    List<BookNumberDTO> findByStudentId(Long studentId);
    BookNumberDTO create(BookNumberRequestDTO request);
    BookNumberDTO update(Long id, BookNumberRequestDTO request);
    void delete(Long id);
    BookNumberDTO markAsFilled(Long id);
    BookNumberDTO markAsHanded(Long id);
}