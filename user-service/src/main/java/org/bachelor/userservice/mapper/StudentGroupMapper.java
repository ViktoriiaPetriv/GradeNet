package org.bachelor.userservice.mapper;

import org.bachelor.userservice.model.dto.StudentGroupDTO;
import org.bachelor.userservice.model.dto.StudentGroupMemberDTO;
import org.bachelor.userservice.model.dto.StudentGroupRequestDTO;
import org.bachelor.userservice.model.entity.StudentGroup;
import org.bachelor.userservice.model.entity.StudentGroupMember;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface StudentGroupMapper {

    StudentGroupDTO toDto(StudentGroup studentGroup);

    @Mapping(target = "id", ignore = true)
    StudentGroup toEntity(StudentGroupRequestDTO dto);

    @Mapping(target = "id", ignore = true)
    void updateEntity(StudentGroupRequestDTO dto, @MappingTarget StudentGroup studentGroup);

    @Mapping(target = "studentGroupId", source = "studentGroup.id")
    @Mapping(target = "studentId", expression = "java(getStudentId(member))")
    @Mapping(target = "studentName", expression = "java(getStudentName(member))")
    @Mapping(target = "studentEmail", expression = "java(getStudentEmail(member))")
    StudentGroupMemberDTO toMemberDto(StudentGroupMember member);

    default String getStudentName(StudentGroupMember member) {
        if (member.getBookNumber() != null && member.getBookNumber().getStudent() != null) {
            var student = member.getBookNumber().getStudent();
            return String.format("%s %s",
                student.getLastName() != null ? student.getLastName() : "",
                student.getFirstName() != null ? student.getFirstName() : "").trim();
        }
        return null;
    }

    default String getStudentEmail(StudentGroupMember member) {
        if (member.getBookNumber() != null && member.getBookNumber().getStudent() != null) {
            return member.getBookNumber().getStudent().getEmail();
        }
        return null;
    }

    default Long getStudentId(StudentGroupMember member) {
        if (member.getBookNumber() != null && member.getBookNumber().getStudent() != null) {
            return member.getBookNumber().getStudent().getId();
        }
        return null;
    }
}
