package org.bachelor.userservice.repository;

import org.bachelor.userservice.model.entity.BookNumber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface BookNumberRepository extends JpaRepository<BookNumber, Long>, JpaSpecificationExecutor<BookNumber> {
    boolean existsByNumber(String number);
    List<BookNumber> findAllByStudentId(Long studentId);
}