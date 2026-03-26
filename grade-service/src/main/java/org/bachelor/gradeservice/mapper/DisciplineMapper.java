package org.bachelor.gradeservice.mapper;

import org.bachelor.gradeservice.model.dto.DisciplineDTO;
import org.bachelor.gradeservice.model.dto.DisciplineRequestDTO;
import org.bachelor.gradeservice.model.entity.Discipline;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DisciplineMapper {

    @Mapping(target = "id", ignore = true)
    Discipline toEntity(DisciplineRequestDTO dto);

    DisciplineDTO toDto(Discipline discipline);
}