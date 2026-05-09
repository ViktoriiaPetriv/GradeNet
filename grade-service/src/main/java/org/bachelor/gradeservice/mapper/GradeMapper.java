package org.bachelor.gradeservice.mapper;

import org.bachelor.gradeservice.model.dto.GradeCreateDTO;
import org.bachelor.gradeservice.model.dto.GradeDTO;
import org.bachelor.gradeservice.model.entity.Grade;
import org.bachelor.gradeservice.model.entity.NationalGrade;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

// mapper/GradeMapper.java
@Mapper(componentModel = "spring")
public interface GradeMapper {

    @Mapping(target = "entryId", source = "entry.id")
    @Mapping(target = "nationalGrade", qualifiedByName = "nationalGradeToString")
    GradeDTO toDTO(Grade grade);

    @Mapping(target = "entry", ignore = true)
    @Mapping(target = "nationalGrade", ignore = true)
    @Mapping(target = "ectsGrade", ignore = true)
    Grade toEntity(GradeCreateDTO dto);

    @Named("nationalGradeToString")
    default String nationalGradeToString(NationalGrade nationalGrade) {
        if (nationalGrade == null) {
            return null;
        }

        return nationalGrade.getDisplayValue();
    }
}