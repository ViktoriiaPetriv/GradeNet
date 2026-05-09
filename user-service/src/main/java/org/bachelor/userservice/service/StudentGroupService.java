package org.bachelor.userservice.service;

import org.bachelor.userservice.model.dto.PageResponse;
import org.bachelor.userservice.model.dto.StudentGroupDTO;
import org.bachelor.userservice.model.dto.StudentGroupMemberDTO;
import org.bachelor.userservice.model.dto.StudentGroupRequestDTO;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface StudentGroupService {
    PageResponse<StudentGroupDTO> findAll(String name, Long specialtyId, Pageable pageable);
    StudentGroupDTO findById(Long id);
    StudentGroupDTO create(StudentGroupRequestDTO request);
    StudentGroupDTO update(Long id, StudentGroupRequestDTO request);
    void delete(Long id);

    List<StudentGroupMemberDTO> findMembers(Long groupId);
    StudentGroupMemberDTO addMember(Long groupId, Long bookNumberId);
    void removeMember(Long groupId, Long bookNumberId);
}
