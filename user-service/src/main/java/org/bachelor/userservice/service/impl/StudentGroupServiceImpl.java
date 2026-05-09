package org.bachelor.userservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.bachelor.userservice.exception.NotFoundException;
import org.bachelor.userservice.exception.ValidationException;
import org.bachelor.userservice.mapper.StudentGroupMapper;
import org.bachelor.userservice.model.dto.PageResponse;
import org.bachelor.userservice.model.dto.StudentGroupDTO;
import org.bachelor.userservice.model.dto.StudentGroupMemberDTO;
import org.bachelor.userservice.model.dto.StudentGroupRequestDTO;
import org.bachelor.userservice.model.entity.BookNumber;
import org.bachelor.userservice.model.entity.StudentGroup;
import org.bachelor.userservice.model.entity.StudentGroupMember;
import org.bachelor.userservice.repository.BookNumberRepository;
import org.bachelor.userservice.repository.StudentGroupMemberRepository;
import org.bachelor.userservice.repository.StudentGroupRepository;
import org.bachelor.userservice.service.StudentGroupService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentGroupServiceImpl implements StudentGroupService {

    private final StudentGroupRepository studentGroupRepository;
    private final StudentGroupMemberRepository studentGroupMemberRepository;
    private final BookNumberRepository bookNumberRepository;
    private final StudentGroupMapper studentGroupMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StudentGroupDTO> findAll(String name, Long specialtyId, Pageable pageable) {
        boolean hasName = name != null && !name.isBlank();
        Page<StudentGroup> page;
        Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        if (specialtyId != null && hasName) {
            page = studentGroupRepository.findBySpecialtyIdAndNameContaining(specialtyId, name, unsorted);
        } else if (specialtyId != null) {
            page = studentGroupRepository.findBySpecialtyId(specialtyId, unsorted);
        } else if (hasName) {
            page = studentGroupRepository.findByNameContainingIgnoreCase(name, pageable);
        } else {
            page = studentGroupRepository.findAll(pageable);
        }
        return PageResponse.of(page.map(studentGroupMapper::toDto));
    }

    @Override
    @Transactional(readOnly = true)
    public StudentGroupDTO findById(Long id) {
        return studentGroupMapper.toDto(getGroupOrThrow(id));
    }

    @Override
    @Transactional
    public StudentGroupDTO create(StudentGroupRequestDTO request) {
        if (studentGroupRepository.existsByName(request.name())) {
            throw new ValidationException("Група з назвою '%s' вже існує".formatted(request.name()));
        }
        return studentGroupMapper.toDto(studentGroupRepository.save(studentGroupMapper.toEntity(request)));
    }

    @Override
    @Transactional
    public StudentGroupDTO update(Long id, StudentGroupRequestDTO request) {
        StudentGroup group = getGroupOrThrow(id);
        if (!group.getName().equals(request.name()) && studentGroupRepository.existsByName(request.name())) {
            throw new ValidationException("Група з назвою '%s' вже існує".formatted(request.name()));
        }
        studentGroupMapper.updateEntity(request, group);
        return studentGroupMapper.toDto(studentGroupRepository.save(group));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        studentGroupRepository.delete(getGroupOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentGroupMemberDTO> findMembers(Long groupId) {
        getGroupOrThrow(groupId);
        return studentGroupMemberRepository.findAllByStudentGroupId(groupId)
                .stream().map(studentGroupMapper::toMemberDto).toList();
    }

    @Override
    @Transactional
    public StudentGroupMemberDTO addMember(Long groupId, Long bookNumberId) {
        StudentGroup group = getGroupOrThrow(groupId);

        BookNumber bookNumber = bookNumberRepository.findById(bookNumberId)
                .orElseThrow(() -> new NotFoundException("Залікову книжку з ID %s не знайдено".formatted(bookNumberId)));

        if (studentGroupMemberRepository.existsByBookNumberId(bookNumberId)) {
            throw new ValidationException("Ця заліковка вже додана до іншої групи");
        }

        if (studentGroupMemberRepository.existsByStudentGroup_IdAndBookNumber_Student_Id(groupId, bookNumber.getStudent().getId())) {
            throw new ValidationException("До цієї групи вже додана інша заліковка цього студента. Один студент може бути в групі тільки з однією залікою");
        }

        StudentGroupMember member = new StudentGroupMember();
        member.setBookNumber(bookNumber);
        member.setStudentGroup(group);

        return studentGroupMapper.toMemberDto(studentGroupMemberRepository.save(member));
    }

    @Override
    @Transactional
    public void removeMember(Long groupId, Long bookNumberId) {
        getGroupOrThrow(groupId);
        StudentGroupMember member = studentGroupMemberRepository.findByBookNumberId(bookNumberId)
                .filter(m -> m.getStudentGroup().getId().equals(groupId))
                .orElseThrow(() -> new NotFoundException(
                        "Студента з книжкою ID %s не знайдено у групі %s".formatted(bookNumberId, groupId)));
        studentGroupMemberRepository.delete(member);
    }

    private StudentGroup getGroupOrThrow(Long id) {
        return studentGroupRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Групу з ID %s не знайдено".formatted(id)));
    }
}
