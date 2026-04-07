package org.bachelor.gradeservice.mapper;

import org.bachelor.gradeservice.model.dto.GradeDTO;
import org.bachelor.gradeservice.model.dto.GradeRequestDTO;
import org.bachelor.gradeservice.model.entity.Grade;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface GradeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "specialtyDiscipline", ignore = true)
    @Mapping(target = "attempt", ignore = true)
    @Mapping(target = "nationalGrade", ignore = true)
    @Mapping(target = "ectsGrade", ignore = true)
    Grade toEntity(GradeRequestDTO dto);

    @Mapping(target = "specialtyDisciplineId", source = "specialtyDiscipline.id")
    GradeDTO toDto(Grade grade);
}
