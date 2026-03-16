package org.bachelor.userservice.mapper;

import org.bachelor.userservice.model.dto.UserDTO;
import org.bachelor.userservice.model.dto.UserProfileDTO;
import org.bachelor.userservice.model.dto.UserRequestDTO;
import org.bachelor.userservice.model.entity.User;
import org.bachelor.userservice.model.entity.UserOrganization;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "orgId", source = "organizations", qualifiedByName = "extractOrgId")
    UserDTO toDto(User user);

    @Mapping(target = "books", ignore = true)
    @Mapping(target = "orgId", source = "organizations", qualifiedByName = "extractOrgId")
    UserProfileDTO toProfileDto(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organizations", ignore = true)
    @Mapping(target = "password", ignore = true)
    User toEntity(UserRequestDTO userRequestDTO);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organizations", ignore = true)
    @Mapping(target = "password", ignore = true)
    void updateEntity(UserRequestDTO dto, @MappingTarget User user);

    @Named("extractOrgId")
    default Long extractOrgId(List<UserOrganization> organizations) {
        if (organizations == null || organizations.isEmpty()) return null;
        return organizations.getFirst().getOrgId();
    }
}
