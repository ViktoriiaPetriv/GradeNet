package org.bachelor.integrationservice.mapper;

import org.bachelor.integrationservice.model.dto.JournalDisciplineStatusDTO;
import org.bachelor.integrationservice.model.dto.JournalStudentStatusDTO;
import org.bachelor.integrationservice.model.journal.JournalDisciplineDTO;
import org.bachelor.integrationservice.model.journal.JournalStudentDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface JournalStatusMapper {

    @Mapping(target = "existsInSystem", ignore = true)
    JournalStudentStatusDTO toStudentStatus(JournalStudentDTO source);

    @Mapping(target = "existsInSystem", ignore = true)
    @Mapping(target = "semester", ignore = true)
    @Mapping(target = "totalHours", ignore = true)
    @Mapping(target = "academicYear", ignore = true)
    @Mapping(target = "attempts", ignore = true)
    JournalDisciplineStatusDTO toDisciplineStatus(JournalDisciplineDTO source);
}
