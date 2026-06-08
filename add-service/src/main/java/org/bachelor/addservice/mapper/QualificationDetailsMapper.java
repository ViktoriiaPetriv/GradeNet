package org.bachelor.addservice.mapper;

import org.bachelor.addservice.model.dto.QualificationDetailsCreateDTO;
import org.bachelor.addservice.model.dto.QualificationDetailsDTO;
import org.bachelor.addservice.model.entity.QualificationDetails;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface QualificationDetailsMapper {

    @Mapping(target = "additionalWorkId", source = "additionalWork.id")
    QualificationDetailsDTO toDTO(QualificationDetails details);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "additionalWork", ignore = true)
    QualificationDetails toEntity(QualificationDetailsCreateDTO dto);
}
