package org.bachelor.gradeservice.mapper;

import org.bachelor.gradeservice.model.dto.HoursDTO;
import org.bachelor.gradeservice.model.dto.HoursRequestDTO;
import org.bachelor.gradeservice.model.entity.Hours;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface HoursMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "specialtyDiscipline", ignore = true)
    Hours toEntity(HoursRequestDTO dto);

    @Mapping(target = "specialtyDisciplineId", source = "specialtyDiscipline.id")
    HoursDTO toDto(Hours hours);
}
