package org.bachelor.addservice.repository;

import org.bachelor.addservice.model.entity.AdditionalWork;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdditionalWorkRepository extends JpaRepository<AdditionalWork, Long> {
    List<AdditionalWork> findAllByCommissionId(Long commissionId);
    List<AdditionalWork> findAllByBookNumberId(Long bookNumberId);
    List<AdditionalWork> findAllByBookNumberIdIn(List<Long> bookNumberIds);

    @Query("""
        SELECT w FROM AdditionalWork w
        WHERE EXISTS (
            SELECT m FROM CommissionMember m
            WHERE m.commission.id = w.commission.id AND m.professorId = :professorId
        ) OR EXISTS (
            SELECT qd FROM QualificationDetails qd
            WHERE qd.additionalWork.id = w.id AND qd.supervisorId = :professorId
        ) OR EXISTS (
            SELECT pd FROM PracticeDetails pd
            WHERE pd.additionalWork.id = w.id AND pd.supervisorId = :professorId
        )
    """)
    List<AdditionalWork> findAllByProfessor(@Param("professorId") Long professorId);

    @Query(value = """
        SELECT w FROM AdditionalWork w
        WHERE (:type IS NULL OR w.type = :type)
          AND (:commissionId IS NULL OR w.commission.id = :commissionId)
        """,
        countQuery = """
        SELECT COUNT(w) FROM AdditionalWork w
        WHERE (:type IS NULL OR w.type = :type)
          AND (:commissionId IS NULL OR w.commission.id = :commissionId)
        """)
    Page<AdditionalWork> findPagedAll(@Param("type") String type, @Param("commissionId") Long commissionId, Pageable pageable);

    @Query(value = """
        SELECT w FROM AdditionalWork w
        WHERE (:type IS NULL OR w.type = :type)
          AND (:commissionId IS NULL OR w.commission.id = :commissionId)
          AND (
              EXISTS (SELECT m FROM CommissionMember m WHERE m.commission.id = w.commission.id AND m.professorId = :professorId)
              OR EXISTS (SELECT qd FROM QualificationDetails qd WHERE qd.additionalWork.id = w.id AND qd.supervisorId = :professorId)
              OR EXISTS (SELECT pd FROM PracticeDetails pd WHERE pd.additionalWork.id = w.id AND pd.supervisorId = :professorId)
          )
        """,
        countQuery = """
        SELECT COUNT(w) FROM AdditionalWork w
        WHERE (:type IS NULL OR w.type = :type)
          AND (:commissionId IS NULL OR w.commission.id = :commissionId)
          AND (
              EXISTS (SELECT m FROM CommissionMember m WHERE m.commission.id = w.commission.id AND m.professorId = :professorId)
              OR EXISTS (SELECT qd FROM QualificationDetails qd WHERE qd.additionalWork.id = w.id AND qd.supervisorId = :professorId)
              OR EXISTS (SELECT pd FROM PracticeDetails pd WHERE pd.additionalWork.id = w.id AND pd.supervisorId = :professorId)
          )
        """)
    Page<AdditionalWork> findPagedByProfessor(@Param("professorId") Long professorId, @Param("type") String type, @Param("commissionId") Long commissionId, Pageable pageable);

    @Query(value = """
        SELECT w FROM AdditionalWork w
        WHERE w.bookNumberId IN :bookNumberIds
          AND (:type IS NULL OR w.type = :type)
          AND (:commissionId IS NULL OR w.commission.id = :commissionId)
        """,
        countQuery = """
        SELECT COUNT(w) FROM AdditionalWork w
        WHERE w.bookNumberId IN :bookNumberIds
          AND (:type IS NULL OR w.type = :type)
          AND (:commissionId IS NULL OR w.commission.id = :commissionId)
        """)
    Page<AdditionalWork> findPagedByManager(@Param("bookNumberIds") List<Long> bookNumberIds, @Param("type") String type, @Param("commissionId") Long commissionId, Pageable pageable);
}
