package org.bachelor.integrationservice.model.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class JournalImportResultDTO {
    private int disciplinesProcessed;
    private int studentsMatched;
    private int studentsUnmatched;
    private int gradesCreated;
    private List<String> unmatchedStudents;
    private List<ImportErrorDTO> errors;
}
