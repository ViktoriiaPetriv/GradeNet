package org.bachelor.orgservice.mapper;

import org.bachelor.orgservice.model.dto.OrganizationRequestDTO;
import org.bachelor.orgservice.model.dto.OrganizationDTO;
import org.bachelor.orgservice.model.entity.Organization;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrganizationMapper {

    @Mapping(target = "parentId", source = "parent.id")
    OrganizationDTO toDto(Organization entity);

    @Mapping(target = "parent", ignore = true)
    Organization toEntity(OrganizationRequestDTO dto);
}
