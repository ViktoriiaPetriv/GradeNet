package org.bachelor.addservice.repository;

import org.bachelor.addservice.model.entity.CommissionMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommissionMemberRepository extends JpaRepository<CommissionMember, Long> {
    List<CommissionMember> findAllByCommissionId(Long commissionId);
    boolean existsByCommissionIdAndProfessorId(Long commissionId, Long professorId);
    boolean existsByCommissionIdAndIsHeadTrue(Long commissionId);
}
