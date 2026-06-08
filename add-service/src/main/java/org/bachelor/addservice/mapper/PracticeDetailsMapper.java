package org.bachelor.addservice.mapper;

import org.bachelor.addservice.model.dto.PracticeDetailsCreateDTO;
import org.bachelor.addservice.model.dto.PracticeDetailsDTO;
import org.bachelor.addservice.model.entity.PracticeDetails;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PracticeDetailsMapper {

    @Mapping(target = "additionalWorkId", source = "additionalWork.id")
    PracticeDetailsDTO toDTO(PracticeDetails details);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "additionalWork", ignore = true)
    PracticeDetails toEntity(PracticeDetailsCreateDTO dto);
}
