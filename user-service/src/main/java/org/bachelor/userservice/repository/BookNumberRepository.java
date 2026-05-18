package org.bachelor.userservice.repository;

import org.bachelor.userservice.model.entity.BookNumber;
import org.bachelor.userservice.model.entity.BookNumberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookNumberRepository extends JpaRepository<BookNumber, Long>, JpaSpecificationExecutor<BookNumber> {
    boolean existsByNumber(String number);
    List<BookNumber> findAllByStudentId(Long studentId);
    boolean existsByStudentIdAndSpecialtyOfferingId(Long studentId, Long specialtyOfferingId);
    boolean existsByStudentIdAndSpecialtyOfferingIdInAndStatusNot(Long studentId, List<Long> offeringIds, BookNumberStatus status);

    @Query("SELECT DISTINCT b.student.id FROM BookNumber b WHERE b.specialtyOfferingId IN :specialtyOfferingIds")
    List<Long> findDistinctStudentIdsBySpecialtyOfferingIdIn(@Param("specialtyOfferingIds") List<Long> specialtyOfferingIds);

    @Query("""
    SELECT b FROM BookNumber b
    WHERE b.specialtyOfferingId = :specialtyOfferingId
      AND YEAR(b.regStartDate) = :enrollYear
    """)
    List<BookNumber> findAllBySpecialtyOfferingIdAndEnrollYear(
            @Param("specialtyOfferingId") Long specialtyOfferingId,
            @Param("enrollYear") int enrollYear);

    List<BookNumber> findAllBySpecialtyOfferingId(Long specialtyOfferingId);
}