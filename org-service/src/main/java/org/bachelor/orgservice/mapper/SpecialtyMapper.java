package org.bachelor.orgservice.mapper;

import org.bachelor.orgservice.model.dto.SpecialtyDTO;
import org.bachelor.orgservice.model.dto.SpecialtyRequestDTO;
import org.bachelor.orgservice.model.entity.Organization;
import org.bachelor.orgservice.model.entity.Specialty;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface SpecialtyMapper {

    @Mapping(target = "orgId", source = "organization.id")
    SpecialtyDTO toDto(Specialty specialty);

    @Mapping(target = "organization", source = "orgId", qualifiedByName = "mapToOrganization")
    Specialty toEntity(SpecialtyRequestDTO dto);

    @Named("mapToOrganization")
    default Organization mapToOrganization(Long id) {
        Organization organization = new Organization();
        organization.setId(id);
        return organization;
    }
}
