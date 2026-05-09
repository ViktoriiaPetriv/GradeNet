package org.bachelor.gradeservice.model.dto;

import lombok.Data;
import org.bachelor.gradeservice.model.entity.EntryResult;
import org.bachelor.gradeservice.model.entity.EntryStatus;

import java.time.LocalDate;

@Data
public class BulkGradeEntryDTO {
    private Long entryId;
    private Long bookNumberId;
    private String studentName;
    private LocalDate reportDate;
    private EntryStatus status;
    private EntryResult result;
    private GradeDTO latestGrade;
}
