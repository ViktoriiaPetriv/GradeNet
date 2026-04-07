package org.bachelor.gradeservice.mapper;

import org.bachelor.gradeservice.model.dto.DisciplineDTO;
import org.bachelor.gradeservice.model.dto.DisciplineRequestDTO;
import org.bachelor.gradeservice.model.entity.Discipline;
import org.bachelor.gradeservice.model.entity.SpecialtyDiscipline;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DisciplineMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "specialtyDisciplines", ignore = true)
    Discipline toEntity(DisciplineRequestDTO dto);

    @Mapping(target = "id", source = "sd.id")
    @Mapping(target = "name", source = "discipline.name")
    @Mapping(target = "specialtyId", source = "sd.specialtyId")
    @Mapping(target = "professorId", source = "sd.professorId")
    @Mapping(target = "reportDate", source = "sd.reportDate")
    DisciplineDTO toDto(Discipline discipline, SpecialtyDiscipline sd);
}