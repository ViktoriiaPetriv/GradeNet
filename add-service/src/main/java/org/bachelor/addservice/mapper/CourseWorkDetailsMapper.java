package org.bachelor.addservice.mapper;

import org.bachelor.addservice.model.dto.CourseWorkDetailsCreateDTO;
import org.bachelor.addservice.model.dto.CourseWorkDetailsDTO;
import org.bachelor.addservice.model.entity.CourseWorkDetails;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CourseWorkDetailsMapper {

    @Mapping(target = "additionalWorkId", source = "additionalWork.id")
    CourseWorkDetailsDTO toDTO(CourseWorkDetails details);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "additionalWork", ignore = true)
    CourseWorkDetails toEntity(CourseWorkDetailsCreateDTO dto);
}
