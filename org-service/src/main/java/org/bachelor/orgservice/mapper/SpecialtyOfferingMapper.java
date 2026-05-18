package org.bachelor.orgservice.mapper;

import org.bachelor.orgservice.model.dto.SpecialtyOfferingDTO;
import org.bachelor.orgservice.model.entity.SpecialtyOffering;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SpecialtyOfferingMapper {

    @Mapping(target = "specialtyId", source = "specialty.id")
    SpecialtyOfferingDTO toDto(SpecialtyOffering entity);
}
