package org.bachelor.addservice.mapper;

import org.bachelor.addservice.model.dto.AdditionalWorkCreateDTO;
import org.bachelor.addservice.model.dto.AdditionalWorkDTO;
import org.bachelor.addservice.model.entity.AdditionalWork;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {CourseWorkDetailsMapper.class, PracticeDetailsMapper.class, QualificationDetailsMapper.class})
public interface AdditionalWorkMapper {

    @Mapping(target = "commissionId", source = "commission.id")
    AdditionalWorkDTO toDTO(AdditionalWork work);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "commission", ignore = true)
    @Mapping(target = "courseWorkDetails", ignore = true)
    @Mapping(target = "practiceDetails", ignore = true)
    @Mapping(target = "qualificationDetails", ignore = true)
    AdditionalWork toEntity(AdditionalWorkCreateDTO dto);
}
