package org.bachelor.userservice.mapper;

import org.bachelor.userservice.model.dto.UserDTO;
import org.bachelor.userservice.model.dto.UserProfileDTO;
import org.bachelor.userservice.model.dto.UserRequestDTO;
import org.bachelor.userservice.model.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDTO toDto(User user);

    @Mapping(target = "studentInfo", ignore = true)
    UserProfileDTO toProfileDto(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organizations", ignore = true)
    @Mapping(target = "password", ignore = true)
    User toEntity(UserRequestDTO userRequestDTO);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organizations", ignore = true)
    @Mapping(target = "password", ignore = true)
    void updateEntity(UserRequestDTO dto, @MappingTarget User user);
}
