package org.bachelor.orgservice.mapper;

import org.bachelor.orgservice.model.dto.OrganizationRequestDTO;
import org.bachelor.orgservice.model.dto.OrganizationDTO;
import org.bachelor.orgservice.model.dto.OrganizationShortDTO;
import org.bachelor.orgservice.model.entity.Organization;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrganizationMapper {

    @Mapping(target = "parentId", source = "parent.id")
    @Mapping(target = "children", source = "children")
    OrganizationDTO toDto(Organization organization);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "children", ignore = true)
    Organization toEntity(OrganizationRequestDTO organizationRequestDTO);

    OrganizationShortDTO toShortDto(Organization organization);
}
