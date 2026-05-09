package org.bachelor.gradeservice.model.dto;

import lombok.Data;
import org.bachelor.gradeservice.model.entity.EntryResult;
import org.bachelor.gradeservice.model.entity.EntryStatus;

import java.util.List;

@Data
public class StudentDisciplineDTO {
    private Long entryId;
    private String disciplineName;
    private Long specialtyDisciplineId;
    private String academicYear;
    private Integer attempt;
    private EntryStatus status;
    private EntryResult result;
    private List<HoursDTO> hours;
    private List<GradeDTO> grades;
}