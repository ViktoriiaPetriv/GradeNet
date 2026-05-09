package org.bachelor.gradeservice.mapper;

import org.bachelor.gradeservice.model.dto.DisciplineCreateDTO;
import org.bachelor.gradeservice.model.dto.DisciplineCreateResponseDTO;
import org.bachelor.gradeservice.model.dto.DisciplineDTO;
import org.bachelor.gradeservice.model.dto.HoursDTO;
import org.bachelor.gradeservice.model.entity.Discipline;
import org.bachelor.gradeservice.model.entity.SpecialtyDiscipline;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {HoursMapper.class})
public interface DisciplineMapper {

    DisciplineDTO toDTO(Discipline discipline);

    Discipline toEntity(DisciplineCreateDTO dto);

    @Mapping(target = "disciplineId", source = "discipline.id")
    @Mapping(target = "name", source = "discipline.name")
    @Mapping(target = "specialtyDisciplineId", source = "specialtyDiscipline.id")
    @Mapping(target = "specialtyId", source = "specialtyDiscipline.specialtyId")
    @Mapping(target = "hours", source = "hours")
    DisciplineCreateResponseDTO toCreateResponseDTO(Discipline discipline, SpecialtyDiscipline specialtyDiscipline, HoursDTO hours);
}
