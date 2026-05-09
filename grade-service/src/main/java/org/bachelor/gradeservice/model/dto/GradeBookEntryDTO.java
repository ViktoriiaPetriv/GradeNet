package org.bachelor.gradeservice.model.dto;

import lombok.Data;
import org.bachelor.gradeservice.model.entity.EntryResult;
import org.bachelor.gradeservice.model.entity.EntryStatus;

import java.time.LocalDate;

@Data
public class GradeBookEntryDTO {
    private Long id;
    private Long bookNumberId;
    private Long specialtyDisciplineId;
    private Long professorId;
    private String academicYear;
    private Integer attempt;
    private EntryStatus status;
    private EntryResult result;
    private LocalDate reportDate;
}
