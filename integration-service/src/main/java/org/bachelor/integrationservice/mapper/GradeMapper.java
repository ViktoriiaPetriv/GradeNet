package org.bachelor.integrationservice.mapper;

import org.bachelor.integrationservice.model.ParsedGrade;
import org.bachelor.integrationservice.model.dto.GradeComparisonDisciplineDTO;
import org.bachelor.integrationservice.model.dto.GradeDataDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface GradeMapper {

    @Mapping(target = "nationalGrade",
             expression = "java(source.getNationalGrade() != null ? source.getNationalGrade().toString() : null)")
    GradeDataDTO toGradeDataDTO(ParsedGrade source);

    GradeComparisonDisciplineDTO toDisciplineDTO(int index, String name);
}
