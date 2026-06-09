package org.bachelor.addservice.repository;

import org.bachelor.addservice.model.entity.Commission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommissionRepository extends JpaRepository<Commission, Long> {

    @Query("""
        SELECT c FROM Commission c
        WHERE EXISTS (
            SELECT m FROM CommissionMember m
            WHERE m.commission.id = c.id AND m.professorId = :professorId
        )
        OR EXISTS (
            SELECT qd FROM QualificationDetails qd
            WHERE qd.additionalWork.commission.id = c.id AND qd.supervisorId = :professorId
        )
        OR EXISTS (
            SELECT pd FROM PracticeDetails pd
            WHERE pd.additionalWork.commission.id = c.id AND pd.supervisorId = :professorId
        )
    """)
    List<Commission> findAllByProfessor(@Param("professorId") Long professorId);

    @Query(value = """
        SELECT c FROM Commission c
        WHERE :status = 'all'
           OR (:status = 'active' AND (c.endDate IS NULL OR c.endDate >= CURRENT_DATE))
           OR (:status = 'inactive' AND c.endDate IS NOT NULL AND c.endDate < CURRENT_DATE)
        """,
        countQuery = """
        SELECT COUNT(c) FROM Commission c
        WHERE :status = 'all'
           OR (:status = 'active' AND (c.endDate IS NULL OR c.endDate >= CURRENT_DATE))
           OR (:status = 'inactive' AND c.endDate IS NOT NULL AND c.endDate < CURRENT_DATE)
        """)
    Page<Commission> findPagedAll(@Param("status") String status, Pageable pageable);

    @Query(value = """
        SELECT c FROM Commission c
        WHERE (
            EXISTS (SELECT m FROM CommissionMember m WHERE m.commission.id = c.id AND m.professorId = :professorId)
            OR EXISTS (SELECT qd FROM QualificationDetails qd WHERE qd.additionalWork.commission.id = c.id AND qd.supervisorId = :professorId)
            OR EXISTS (SELECT pd FROM PracticeDetails pd WHERE pd.additionalWork.commission.id = c.id AND pd.supervisorId = :professorId)
        ) AND (
            :status = 'all'
            OR (:status = 'active' AND (c.endDate IS NULL OR c.endDate >= CURRENT_DATE))
            OR (:status = 'inactive' AND c.endDate IS NOT NULL AND c.endDate < CURRENT_DATE)
        )
        """,
        countQuery = """
        SELECT COUNT(c) FROM Commission c
        WHERE (
            EXISTS (SELECT m FROM CommissionMember m WHERE m.commission.id = c.id AND m.professorId = :professorId)
            OR EXISTS (SELECT qd FROM QualificationDetails qd WHERE qd.additionalWork.commission.id = c.id AND qd.supervisorId = :professorId)
            OR EXISTS (SELECT pd FROM PracticeDetails pd WHERE pd.additionalWork.commission.id = c.id AND pd.supervisorId = :professorId)
        ) AND (
            :status = 'all'
            OR (:status = 'active' AND (c.endDate IS NULL OR c.endDate >= CURRENT_DATE))
            OR (:status = 'inactive' AND c.endDate IS NOT NULL AND c.endDate < CURRENT_DATE)
        )
        """)
    Page<Commission> findPagedByProfessor(@Param("professorId") Long professorId, @Param("status") String status, Pageable pageable);
}
