package org.bachelor.addservice.mapper;

import org.bachelor.addservice.model.dto.CommissionCreateDTO;
import org.bachelor.addservice.model.dto.CommissionDTO;
import org.bachelor.addservice.model.entity.Commission;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {CommissionMemberMapper.class})
public interface CommissionMapper {

    CommissionDTO toDTO(Commission commission);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "orgId", ignore = true)
    @Mapping(target = "members", ignore = true)
    @Mapping(target = "additionalWorks", ignore = true)
    Commission toEntity(CommissionCreateDTO dto);
}
