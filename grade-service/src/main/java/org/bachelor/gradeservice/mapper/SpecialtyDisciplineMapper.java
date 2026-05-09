package org.bachelor.gradeservice.mapper;

import org.bachelor.gradeservice.model.dto.SpecialtyDisciplineDTO;
import org.bachelor.gradeservice.model.entity.SpecialtyDiscipline;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {DisciplineMapper.class, HoursMapper.class})
public interface SpecialtyDisciplineMapper {

    @Mapping(target = "hours", source = "hours")
    SpecialtyDisciplineDTO toDTO(SpecialtyDiscipline specialtyDiscipline);
}
