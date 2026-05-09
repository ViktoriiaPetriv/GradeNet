package org.bachelor.userservice.repository;

import org.bachelor.userservice.model.entity.StudentGroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentGroupMemberRepository extends JpaRepository<StudentGroupMember, Long> {
    Optional<StudentGroupMember> findByBookNumberId(Long bookNumberId);
    List<StudentGroupMember> findAllByStudentGroupId(Long studentGroupId);
    boolean existsByBookNumberId(Long bookNumberId);
    boolean existsByStudentGroup_IdAndBookNumber_Student_Id(Long groupId, Long studentId);
}
