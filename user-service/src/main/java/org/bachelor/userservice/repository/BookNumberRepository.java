package org.bachelor.userservice.repository;

import org.bachelor.userservice.model.entity.BookNumber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookNumberRepository extends JpaRepository<BookNumber, Long>, JpaSpecificationExecutor<BookNumber> {
    boolean existsByNumber(String number);
    List<BookNumber> findAllByStudentId(Long studentId);
    boolean existsByStudentIdAndSpecialtyId(Long studentId, Long specialtyId);

    @Query("SELECT DISTINCT b.student.id FROM BookNumber b WHERE b.specialtyId IN :specialtyIds")
    List<Long> findDistinctStudentIdsBySpecialtyIdIn(@Param("specialtyIds") List<Long> specialtyIds);

    @Query("""
    SELECT b FROM BookNumber b
    WHERE b.specialtyId = :specialtyId
      AND YEAR(b.regStartDate) = :enrollYear
    """)
    List<BookNumber> findAllBySpecialtyIdAndEnrollYear(
            @Param("specialtyId") Long specialtyId,
            @Param("enrollYear") int enrollYear);

    List<BookNumber> findAllBySpecialtyId(Long specialtyId);
}