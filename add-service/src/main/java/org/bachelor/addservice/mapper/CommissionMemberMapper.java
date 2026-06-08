package org.bachelor.addservice.mapper;

import org.bachelor.addservice.model.dto.CommissionMemberCreateDTO;
import org.bachelor.addservice.model.dto.CommissionMemberDTO;
import org.bachelor.addservice.model.entity.CommissionMember;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CommissionMemberMapper {

    @Mapping(target = "commissionId", source = "commission.id")
    CommissionMemberDTO toDTO(CommissionMember member);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "commission", ignore = true)
    CommissionMember toEntity(CommissionMemberCreateDTO dto);
}
