package org.bachelor.gradeservice.mapper;

import org.bachelor.gradeservice.model.dto.GradeBookEntryDTO;
import org.bachelor.gradeservice.model.dto.HoursDTO;
import org.bachelor.gradeservice.model.dto.StudentDisciplineDTO;
import org.bachelor.gradeservice.model.entity.GradeBookEntry;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Set;

@Mapper(componentModel = "spring")
public interface GradeBookEntryMapper {

    @Mapping(target = "specialtyDisciplineId", source = "specialtyDiscipline.id")
    GradeBookEntryDTO toDTO(GradeBookEntry entry);

    // mapper/GradeBookEntryMapper.java
    @Mapping(target = "disciplineName", source = "entry.specialtyDiscipline.discipline.name")
    @Mapping(target = "specialtyDisciplineId", source = "entry.specialtyDiscipline.id")
    @Mapping(target = "hours", source = "filteredHours")
    @Mapping(target = "grades", source = "entry.grades")
    StudentDisciplineDTO toStudentDisciplineDTO(GradeBookEntry entry, Set<HoursDTO> filteredHours);
}
