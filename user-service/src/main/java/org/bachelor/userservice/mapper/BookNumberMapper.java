package org.bachelor.userservice.mapper;

import org.bachelor.userservice.model.dto.BookNumberDTO;
import org.bachelor.userservice.model.dto.BookNumberRequestDTO;
import org.bachelor.userservice.model.entity.BookNumber;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface BookNumberMapper {

    @Mapping(target = "studentId", source = "student.id")
    BookNumberDTO toDto(BookNumber bookNumber);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "student", ignore = true)
    BookNumber toEntity(BookNumberRequestDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "student", ignore = true)
    void updateEntity(BookNumberRequestDTO dto, @MappingTarget BookNumber bookNumber);
}
