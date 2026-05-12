package org.bachelor.gradeservice.model.dto;

import lombok.Data;
import org.bachelor.gradeservice.model.entity.EntryResult;
import org.bachelor.gradeservice.model.entity.EntryStatus;

@Data
public class GradeBookEntryFilter {
    private Long bookNumberId;
    private Long specialtyDisciplineId;
    private Long professorId;
    private String academicYear;
    private EntryStatus status;
    private EntryResult result;
    private Integer semester;
}
