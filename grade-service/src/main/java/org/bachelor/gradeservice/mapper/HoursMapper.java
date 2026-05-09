package org.bachelor.gradeservice.mapper;

import org.bachelor.gradeservice.model.dto.HoursCreateDTO;
import org.bachelor.gradeservice.model.dto.HoursDTO;
import org.bachelor.gradeservice.model.entity.Hours;
import org.bachelor.gradeservice.model.entity.HoursTemplate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface HoursMapper {

    @Mapping(target = "ectsCredits",     source = "template.ectsCredits")
    @Mapping(target = "totalHours",      source = "template.totalHours")
    @Mapping(target = "classroomHours",  source = "template.classroomHours")
    @Mapping(target = "lectureHours",    source = "template.lectureHours")
    @Mapping(target = "seminarHours",    source = "template.seminarHours")
    @Mapping(target = "laboratoryHours", source = "template.laboratoryHours")
    @Mapping(target = "individualHours", source = "template.individualHours")
    @Mapping(target = "selfWorkHours",   source = "template.selfWorkHours")
    HoursDTO toDTO(Hours hours);

    HoursTemplate toTemplate(HoursCreateDTO dto);
}
